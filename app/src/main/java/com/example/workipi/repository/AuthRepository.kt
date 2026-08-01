package com.example.workipi.repository

import android.util.Log
import com.example.workipi.data.model.AppPermission
import com.example.workipi.data.model.Firma
import com.example.workipi.data.model.InvitationCodeInsert
import com.example.workipi.data.model.InvitationCodeLucrareInsert
import com.example.workipi.data.model.PermissionRow
import com.example.workipi.data.model.User
import com.example.workipi.di.AdminAuthClient
import com.example.workipi.util.InvitationCodeGenerator
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.Clock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.days

private const val TAG = "AuthRepository"
@Singleton
class AuthRepository @Inject constructor(
    private val client: SupabaseClient,
    @AdminAuthClient private val adminClient: SupabaseClient,
    private val userRepository: UserRepository,
    private val codeRepository: InvitationCodeRepository,
    private val codeSkillRepository: InvitationCodeLucrareRepository,
) {

    /**
     * Restaureaza sesiunea salvata local de Supabase Auth (daca exista) si returneaza
     * profilul utilizatorului. Returneaza null daca nu exista sesiune valida.
     * Folosit la pornirea aplicatiei ca sa ramana userul logat.
     */
    suspend fun restoreSession(): User? {
        client.auth.awaitInitialization()
        val authUserId = client.auth.currentUserOrNull()?.id ?: return null
        return userRepository.findByAuthId(authUserId)
    }

    suspend fun signIn(email: String, password: String): Result<User> = runCatching {
        client.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }

        val authUserId = client.auth.currentUserOrNull()?.id
            ?: error("Nu s-a putut obtine ID-ul utilizatorului dupa autentificare.")

        userRepository.findByAuthId(authUserId)
            ?: error("Contul a fost autentificat dar profilul nu a fost gasit. Contacteaza administratorul.")
    }

    suspend fun getCurrentPermissions(): Set<AppPermission> {
        val permissions = client.postgrest
            .rpc("get_my_permissions")
            .decodeList<PermissionRow>()
            .mapNotNull { AppPermission.fromDbValue(it.permission) }
            .toSet()
        check(permissions.isNotEmpty()) {
            "Contul nu are permisiuni configurate. Contacteaza administratorul."
        }
        return permissions
    }

    // Atentie: aceasta metoda creeaza atat contul de autentificare (in Supabase Auth) cat si profilul de utilizator (in tabela "utilizatori") si firma asociata. Daca ceva esueaza pe parcurs, se va incerca stergerea contului de autentificare pentru a evita situatii de conturi "orfani" care nu pot fi folosite.
    suspend fun signUpAdmin(
        companyName: String,
        fullName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): Result<User> = runCatching {
        Log.d(TAG, "Inceput proces inregistrare admin: firma='$companyName', numePrenume='$fullName', email='$email', telefon='$phoneNumber'")

        client.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
        }

        try {
            client.postgrest.rpc(
                "bootstrap_company",
                buildJsonObject {
                    put("p_company_name", companyName.trim())
                    put("p_full_name", fullName.trim())
                    put("p_email", email.trim())
                    put("p_phone", phoneNumber.trim())
                },
            )
                .decodeSingle<User>()
        } catch (e: Throwable) {
            runCatching { client.auth.signOut() }
            throw e
        }
    }

    /**
     * This method is inserting the employee into the database
     * [role] - can be Employee or Engineer
     */
    suspend fun signUpEmployee(
        invitationCode: String,
        email: String,
        password: String,
    ): Result<User> = runCatching {
        Log.d(TAG, "Inceput proces de activare prin invitatie")

        client.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
        }

        try {
            client.postgrest.rpc(
                "claim_invitation",
                buildJsonObject {
                    put("p_code", invitationCode.trim().uppercase())
                    put("p_needs_password_change", false)
                },
            )
                .decodeSingle<User>()
        } catch (e: Throwable) {
            runCatching { client.auth.signOut() }
            throw e
        }
    }

    fun getCurrentUserId(): String {
        return client.auth.currentUserOrNull()?.id ?: error("Cont creat dar nu s-a putut auto-autentifica.")
    }

    /**
     * Adminul creeaza COMPLET contul angajatului (cont de autentificare + profil in "utilizatori" +
     * skills), setand o parola initiala. Angajatul ramane "in asteptare" (necesita_schimbare_parola =
     * true) pana isi schimba parola la prima logare, dar randul exista deja in "utilizatori", deci
     * poate fi folosit imediat in proiecte.
     *
     * Crearea se face pe clientul izolat [adminClient]: signUp-ul logheaza noul angajat DOAR pe acel
     * client (sesiune in memorie), iar insert-urile se fac sub sesiunea lui, deci respecta acelasi RLS
     * "randul propriu" ca la auto-activare. Sesiunea adminului de pe clientul principal ramane neatinsa.
     */
    suspend fun createEmployeeAccountAsAdmin(
        fullName: String,
        email: String,
        phoneNumber: String,
        role: String,
        companyId: Long,
        password: String,
        salary: Float?,
        skills: List<Pair<Long, String>>,
    ): Result<User> = runCatching {
        val invitationCode = InvitationCodeGenerator.generate()
        val invitation = codeRepository.generateInvitationCode(
            InvitationCodeInsert(
                code = invitationCode,
                companyId = companyId,
                role = role,
                email = email.trim(),
                phoneNumber = phoneNumber.trim(),
                fullName = fullName.trim(),
                expirationDate = Clock.System.now() + 7.days,
                salary = salary,
            )
        )
        if (skills.isNotEmpty()) {
            codeSkillRepository.assignSkillsToCode(
                skills.map { (skillId, level) ->
                    InvitationCodeLucrareInsert(
                        codeId = invitation.id,
                        idLucrare = skillId,
                        skillLevel = level,
                    )
                }
            ).getOrThrow()
        }

        try {
            adminClient.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            adminClient.postgrest.rpc(
                "claim_invitation",
                buildJsonObject {
                    put("p_code", invitationCode)
                    put("p_needs_password_change", true)
                },
            ).decodeSingle<User>()
        } catch (e: Throwable) {
            codeRepository.deleteByCode(invitationCode)
            throw e
        } finally {
            // Curatam sesiunea izolata, indiferent de rezultat.
            runCatching { adminClient.auth.signOut() }
        }
    }

    /**
     * Schimba parola utilizatorului logat in prezent (pe clientul principal) si marcheaza contul ca
     * activat (necesita_schimbare_parola = false). Folosit la prima logare a angajatului.
     */
    suspend fun changeOwnPassword(newPassword: String): Result<Unit> = runCatching {
        client.auth.updateUser { password = newPassword }
        val authId = getCurrentAuthUser()
        client.from("utilizatori").update(
            { set("necesita_schimbare_parola", false) }
        ) {
            filter { eq("auth_utilizator_id", authId) }
        }
    }

    suspend fun getCompanyName(companyId: Long): String? =
        client.from("firme")
            .select { filter { eq("id_firma", companyId) } }
            .decodeSingleOrNull<Firma>()
            ?.denumire


    fun getCurrentAuthUser(): String {
        return client.auth.currentUserOrNull()?.id
            ?: error("Nu esti autentificat")
    }

    suspend fun getCompanyIdFromAuthUser(): Long {
        val authId = getCurrentAuthUser()
        val admin = userRepository.findByAuthId(authId)
            ?: error("Profil admin negasit")
        val companyId = admin.idCompany
            ?: error("Userul nu este intr-o firma")

        return companyId
    }

    suspend fun signOut() {
        client.auth.signOut()
    }
}

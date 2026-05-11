package com.example.workipi.repository

import android.util.Log
import com.example.workipi.data.model.Firma
import com.example.workipi.data.model.FirmaInsert
import com.example.workipi.data.model.User
import com.example.workipi.data.model.UtilizatorInsert
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AuthRepository"
@Singleton
class AuthRepository @Inject constructor(
    private val client: SupabaseClient,
    private val userRepository: UserRepository,
    private val codeRepository: InvitationCodeRepository
) {

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

    // Atentie: aceasta metoda creeaza atat contul de autentificare (in Supabase Auth) cat si profilul de utilizator (in tabela "utilizatori") si firma asociata. Daca ceva esueaza pe parcurs, se va incerca stergerea contului de autentificare pentru a evita situatii de conturi "orfani" care nu pot fi folosite.
    suspend fun signUpAdmin(
        companyName: String,
        fullName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): Result<User> = runCatching {
        Log.d(TAG, "Inceput proces inregistrare admin: firma='$companyName', numePrenume='$fullName', email='$email', telefon='$phoneNumber'")

        val companyLabelsStatus: CompanyLabelsStatus = checkIfLabelsAreValid(companyName, email, phoneNumber)
        when (companyLabelsStatus) {
            CompanyLabelsStatus.PHONE_ALREADY_USED ->
                error("Numarul de telefon '$phoneNumber' este deja folosit de un alt cont.")
            CompanyLabelsStatus.EMAIL_ALREADY_USED ->
                error("Email-ul '$email' este deja folosit de un alt cont.")
            CompanyLabelsStatus.COMPANY_NAME_ALREADY_USED ->
                error("Exista deja o firma inregistrata cu numele '$companyName'. Te rugam sa alegi alt nume.")
            else -> Log.d(TAG, "Verificare etichete companie: toate etichetele sunt valide.")

        }

        client.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
        }

        val authUserId = getCurrentUserId()
        try {
            val firma = client.from("firme")
                .insert(FirmaInsert(denumire = companyName.trim())) { select() }
                .decodeSingle<Firma>()

            // Daca am ajuns aici, inseamna ca firma a fost creata cu succes. Acum putem crea utilizatorul admin.
            userRepository.insertUserAccount(
                UtilizatorInsert(
                    numePrenume = fullName.trim(),
                    email = email.trim(),
                    numarTelefon = phoneNumber,
                    rol = "admin",
                    idFirma = firma.idFirma,
                    authUtilizatorId = authUserId,
                )
            )
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
        fullName: String,
        phoneNumber: String,
        email: String,
        role: String,
        companyId: Long,
        password: String,
        salary: Float? = null,
    ): Result<User> = runCatching {
        Log.d(TAG, "Inceput proces de inregistrare a angajatului $fullName")

        client.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
        }

        val authUserId = getCurrentUserId()

        try {
            userRepository.insertUserAccount(
                UtilizatorInsert(
                    numePrenume = fullName.trim(),
                    email = email.trim(),
                    numarTelefon = phoneNumber,
                    rol = role,
                    idFirma = companyId,
                    authUtilizatorId = authUserId,
                    salariu = salary,
                )
            )
        } catch (e: Throwable) {
            runCatching { client.auth.signOut() }
            throw e
        }
    }

    fun getCurrentUserId(): String {
        return client.auth.currentUserOrNull()?.id ?: error("Cont creat dar nu s-a putut auto-autentifica.")
    }


    /**
     * We are cheching if the phone number and the email is valid and not
     * already existing in the database
     */
    private suspend fun checkIfLabelsAreValid(
        companyName: String,
        email: String,
        phoneNumber: String,
    ): CompanyLabelsStatus {
        // TODO: we need to check also if the company name already exists
        val foundUserByPhone = userRepository.findByPhoneNumber(phoneNumber)
        val foundUserByEmail = userRepository.findByEmail(email)

        return when {
            foundUserByPhone != null -> {
                Log.d(TAG, "Numar de telefon deja folosit: $phoneNumber")
                CompanyLabelsStatus.PHONE_ALREADY_USED
            }
            foundUserByEmail != null -> {
                Log.d(TAG, "Email deja folosit: $email")
                CompanyLabelsStatus.EMAIL_ALREADY_USED
            }
            else -> CompanyLabelsStatus.VALID
        }
    }

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

    private enum class CompanyLabelsStatus {
        VALID,
        PHONE_ALREADY_USED,
        EMAIL_ALREADY_USED,
        COMPANY_NAME_ALREADY_USED,
    }

    suspend fun signOut() {
        client.auth.signOut()
    }
}
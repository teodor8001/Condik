# Progres peste noapte — WorkIPI

Build verde la fiecare pas (`./gradlew compileDebugKotlin`). **Niciun commit** (le faci tu manual).

## ✅ Terminat

### AddProject (creare proiect)
- **Scoase zonele** complet din ecran (toggle „Proiect cu zone" + tabelul „Zone proiect"). Zonele se adauga acum din ecranul de detaliu. La creare se pune automat o **zona implicita** (necesara in DB).
- **Adaugata data de start** (calendar), pe langa termenul de finalizare. (`ProjectInsert` trimite acum `termen_inceput`.)

### ProjectDetail
- **Echipa**: exclude adminii — doar angajati + ingineri. Salariul total al echipei nu mai include adminii.
- **Card Zone** (in locul „Riscuri"): click -> popup cu lista zonelor (% + mp). Buton **„+"** sus-dreapta -> formular „Adauga zona" (doar nume). Fiecare zona are **Edit** (nume + suprafata) si **Delete**.
- **Card Mix lucrari**: click -> popup. Buton **„+"** -> formular „Adauga lucrare":
  - dropdown **Zona** (ascuns daca proiectul n-are zone reale -> foloseste zona implicita),
  - dropdown **Lucrare** = skillurile firmei + optiunea **„+ Creeaza lucrare noua"** (creeaza skill in firma -> apare in Preturi),
  - **cantitate**; unitatea (mp/mc/buc) se ia automat din skill.
  - Adaugarea unei lucrari **creste cantitatea totala a proiectului** (incrementeaza suprafata zonei tinta).
- **Card Pontari**: click -> **lista pontarilor** facute (angajat, lucrare, zona, data, cantitate). Buton **„+"** -> formularul de creare pontare (acelasi ca inainte). Dupa salvare, lista se reincarca.
- **Popup Echipa**: latime plafonata (nu mai e lat pe tot ecranul); mini-tabel cu coloane Nume complet / Salariu / Medie mp/zi, in stilul listei de proiecte.
- Toate popup-urile de lista au latime decenta (max 460dp).

### Lista proiecte
- **Sortare prin click pe capul de coloana** (Nume, Progres, Mp realizati, Medie mp/zi, Nr. revizii, Riscuri): click = crescator, click din nou = descrescator, cu sageata indicatoare. (Regula generala salvata in memorie — celelalte liste se aliniaza pe masura ce le ating.)
- **Stergere proiect**: tomberon in dreapta fiecarui rand -> dialog „Esti sigur?" cu „Da" (neutru, sterge) / „Nu" (portocaliu).

### Numele firmei (item 10 — partial)
- Numele firmei se afiseaza in **header-ul ecranului Acasa** (deasupra „Buna ziua, ..."), adus din tabela `firme`. Locatia ramane de confirmat (ziceai ca inca nu te-ai hotarat) — e usor de mutat.

### Alte (din sesiuni anterioare aceeasi seara)
- Login persistent (ramai logat dupa restart); logout face si signOut din Supabase.
- Fix navigare: „Proiecte" din meniu te scotea corect din ProjectDetail.

## ✅ Adaugat in runda 2

- **Editare profil angajat**: in EmployeeDetail, buton Edit (creion) -> dialog cu nume, telefon, rol, **salariu**. (`UserRepository.updateEmployee` + VM.)
- **AddProject**: in loc de sageata inapoi, **X** in dreapta sus; la apasare intreaba „Renunti la proiect?" (ca sa nu pierzi din greseala ce ai introdus).
- **Editare lucrari** (nu doar zone): fiecare intrare din Mix lucrari are Edit (cantitate) si Delete.
- **Confirmare la stergere — regula GENERALA**: dialog reutilizabil `ConfirmDialog` (ui/components). Stergerea de zona si de lucrare cere acum „Esti sigur?". (Stergerea de proiect avea deja confirmare.)
- **Lista pontari = tabel sortabil**: coloane Angajat / Lucrare / Zi / Cant., click pe coloana sorteaza (toggle asc/desc), in stilul listei de proiecte.
- **Bare de jos pe tablete**: adaugat `navigationBarsPadding` global in drawer, ca bara de navigatie sa nu mai manance din ecran.

## ✅ Adaugat in runda 3

- **Sumar oferta (AddProject)**: card live care calculeaza, pe masura ce selectezi angajati / termene / lucrari:
  - **Cheltuieli salarii** = durata (zile, start->finalizare) × suma salariilor zilnice (salariu/30) ale angajatilor selectati.
  - **Recomandare personal**: sectiune noua „Lucrari cerute" (dropdown skill firma + cantitate). Pentru fiecare lucrare se ia **media mp/zi a angajatilor care stiu sa o faca** (din `istoric_pontari`, via `getBySkill`), iar recomandarea = ceil(cantitate / (medie_mp_zi × zile)). Se afiseaza si totalul recomandat.
  - Lucrarile cerute se si **salveaza** pe proiect (pe zona implicita), deci apar dupa creare in Mix lucrari.
- **Guard la navigare din ecran de editare**: daca esti pe un ecran de editare/adaugare (AddProject, AddEmployee, ManageSkills, AssignEmployees, Pontare) si dai pe alt item din meniu, intreaba „Renunti la modificari?" inainte sa plece. Plus **BackHandler** pe AddProject (butonul back fizic cere aceeasi confirmare).
- (Reconfirmat) **AddProject X + confirmare** era deja facut in runda 2.

## ✅ Adaugat in runda 4

- **Oferta se salveaza corect ca oferta**: `AddProject` primeste flag `offer` prin ruta (FAB din Ofertare -> offer=true, din Proiecte -> false). Label-uri „Adauga/Salveaza oferta", navigheaza inapoi in Ofertare. Inainte salva ca proiect normal.
- **Card Sumar oferta**: culoare = surface (ca toate cardurile), nu mai iese din paleta.
- **Avertizare pontare duplicata**: la salvare, daca exista deja o pontare cu acelasi angajat + lucrare + zi in proiect, apare dialog „Pontare duplicata — adaugi oricum?" (Da/Nu). Functioneaza si pe ecranul de pontare, si pe popup-ul din Detalii proiect.
- **Validari (regex)** in `util/Validation.kt`: email (trebuie `@` + domeniu), telefon (exact 10 cifre, doar numere). Aplicat la: invitare angajat, inregistrare admin (firma noua), editare profil angajat.
- **Grafic cu navigare in timp** — component partajat `ui/components/TimeBarChart.kt` (`TimeNavBarChart`): dropdown granularitate (Saptamana/Luna/An) + sageti stanga/dreapta intre perioade (fara slide), bare + etichete. Folosit ACUM in **ambele** locuri: graficul din **Detalii proiect** si „Productie mp/zi" din **Acasa** (fostul grafic per-zi). Fara duplicare de cod.

## ⏳ Ramas / de clarificat dimineata

- **Item 4 — toggle „are materiale" + tab Materiale**: necesita coloana noua in DB (`proiecte.are_materiale`) si un tab de Materiale care inca nu exista. De decis schema.
- **Item 7 — nickname angajat in top**: necesita coloana `porecla` in `utilizatori`. N-am adaugat ca sa nu stric decodarea. De adaugat coloana, apoi leg UI (editare in profil + afisare in leaderboard inainte de nume).
- **Item 8 — detalii angajat + sortare in profil**: am adaugat EDITAREA (inclusiv salariu). Mai ramane partea de detalii suplimentare (vechime/eficienta/rank) + sortare pe ele in profil. (vechime/eficienta = calcule noi.)
- **Item 11 — card „Firma" (skilluri/lucrari, unelte, materiale)**: feature mare, necesita tabele noi (`unelte`, `materiale`, evidenta folosire unelte + de cine). De proiectat schema.

## Note
- „Riscuri" a fost inlocuit cu „Zone" pe cardul din detaliu (cum ai cerut intr-o sesiune anterioara).
- Pentru lucrarea noua creata din proiect am pus punctaj 0 implicit (se poate edita ulterior din Preturi).

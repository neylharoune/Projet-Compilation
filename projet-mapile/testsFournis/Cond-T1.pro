programme testCondImbrique :
var
    ent heure, typeVehicule, prix;
    bool estEtudiant;

debut
    heure := 14;
    typeVehicule := 1;
    estEtudiant := vrai;
    prix := 0;

    cond
        (heure < 12) : 
            prix := 10;
        
        (heure >= 12) : 
            cond
                (typeVehicule = 1) : 
                    si estEtudiant alors
                        prix := 15;
                    sinon
                        prix := 25;
                    fsi;
                
                (typeVehicule = 2) : 
                    prix := 8;
                
                aut : 
                    prix := 50;
            fcond;
            
        aut : 
            prix := 0;
    fcond;

    ecrire(prix);
fin
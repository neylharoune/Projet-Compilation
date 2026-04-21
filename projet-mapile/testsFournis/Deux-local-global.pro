programme TestNbParams :

var
    ent x;

proc testNb
    fixe(ent a, b)
    mod(ent c)
    const x = 2;
debut
    c := a + b;
fin;

debut
    x := 10;

    # --- L'APPEL CORRECT (2 fixes, 1 mod) ---
    testNb(5, 5)(x); 
        ecrire(999);
fin
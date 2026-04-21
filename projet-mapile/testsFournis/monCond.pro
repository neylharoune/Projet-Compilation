programme TestProcValide :

var
    ent val1, val2, maSomme, monProduit;

# Procédure qui fait des maths pour tester les variables locales et les paramètres
proc Calculer fixe(ent a, b) mod(ent resSum; ent resProd)
var
    ent variableLocale; # Juste pour vérifier que l'allocation mémoire se passe bien
debut
    # On utilise une variable locale pour le calcul
    variableLocale := a + b;
    resSum := variableLocale;
    
    # On affecte directement le paramètre mod
    resProd := a * b
fin;

debut
    val1 := 5;
    val2 := 4;
    
    # Appel de la procédure
    Calculer (val1, val2) (maSomme, monProduit);
    
    # Si tout marche, Mapile DOIT afficher 9 puis 20 !
    ecrire(maSomme, monProduit)
fin
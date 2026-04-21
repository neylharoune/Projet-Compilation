programme deuxièmeTestSi: {Imbrication de si}
const m=10;
var ent n, s;
debut
     n:=2; 
     si n=0 alors
         n:=m;
            si n=2 alors 
                ecrire(1); 
            fsi; 
     sinon 
            n:= 5;

            si n=2 alors 
                ecrire(2); 
            sinon 
                 ecrire(3);
            fsi; 

         ecrire(4); 
     fsi;
        ecrire(5); 
fin

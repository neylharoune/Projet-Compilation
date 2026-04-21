package compilateur;

import java.security.Policy;

import org.antlr.analysis.SemanticContext.FalsePredicate;

import analyseurs.UtilLex;
import edl.*;
import libIO.*;

/**
 * classe de mise en oeuvre du compilateur
 * =======================================
 * (verifications semantiques + production du code objet)
 * 
 * @author Girard, Masson, Perraudeau
 *
 */

public class PtGen {

	// Renseigner ici un nom pour le trinome, constitué UNIQUEMENT DE LETTRES
	public static String trinome = "ChabiGloriaNeyla"; // TODO

	// taille max de la table des symboles
	private static final int MAXSYMB = 300;

	// TABLE DES SYMBOLES
	private static EltTabSymb[] tabSymb = new EltTabSymb[MAXSYMB + 1];
	private static int it; // indice de remplissage de tabSymb
	private static int bc; // bloc courant (=1 si le bloc courant est le programme principal)

	// codes MAPILE
	private static final int RESERVER = 1, EMPILER = 2, CONTENUG = 3, AFFECTERG = 4,
			OU = 5, ET = 6, NON = 7, INF = 8, INFEG = 9, SUP = 10, SUPEG = 11, EG = 12, DIFF = 13,
			ADD = 14, SOUS = 15, MUL = 16, DIV = 17,
			BSIFAUX = 18, BINCOND = 19,
			LIRENT = 20, LIREBOOL = 21, ECRENT = 22, ECRBOOL = 23,
			ARRET = 24,
			EMPILERADG = 25, EMPILERADL = 26, CONTENUL = 27, AFFECTERL = 28, APPEL = 29, RETOUR = 30;

	// codes des valeurs vrai/faux
	private static final int VRAI = 1, FAUX = 0;

	// types permis
	private static final int ENT = 1, BOOL = 2, NEUTRE = 3;

	// categories des identificateurs
	private static final int CONSTANTE = 1, VARGLOBALE = 2, VARLOCALE = 3,
			PARAMFIXE = 4, PARAMMOD = 5, PROC = 6, DEF = 7, REF = 8, PRIVEE = 9;

	// production du code objet en memoire
	private static ProgObjet po;

	// pile de reprise pour les branchements en avant et en arrière
	private static TPileRep pileRep;

	// compilation des littéraux (entier ou booléen)
	private static int vCour;

	// contrôle de type : type de l'expression compilee
	private static int tCour;

	// Variables de recupération des infos 
	private static int type;
	private static int categorie; 
	private static int code; 
	private static int info;
	private static int addrSymb;
	private static int longVar; // Variable pour compter le nombre de réservation à faire
	private static int present;
	private static int typeVar;// Variable pour verifier le type d'une affectation, d'une lecture, d'une ecriture
	private static int indexVar; // Variable pour stocker l'index d'un ident
	private static int indexBSIF; //index du dernier trou bsifaux
	private static int indexBINC; //index du dernier trou binCond
	private static boolean marqSinon; // Marqueur pour savoir si on est passé dans un sinon
	private static int majparam; //Mise a  jour du nombre de param de procedure
	private static int nbrVarProc; // Compteur de paraméètre et variable local de procédures
	private static int nbrVarLoc; //Compteur variables locales seules
	private static int indexAffect;
	private static int indexSautProcs = -1;
	private static int addrProcAppel; // Adresse de la procédure qu'on est en train d'appeler
	private static int indexParam;    // Compteur pour savoir à quel paramètre on en est
	
	// TABLE DES SYMBOLES
	// ----------------------
	/**
	 * utilitaire de recherche de l'ident courant (ayant pour code
	 * UtilLex.numIdCourant) dans tabSymb
	 * 
	 * @param borneInf recherche de l'indice it vers borneInf (=1 si recherche
	 *                 dans tout tabSymb)
	 * @return indice de l'ident courant (de code UtilLex.numIdCourant) dans
	 *         tabSymb (O si absence)
	 */
	private static int presentIdent(int borneInf) {
		int i = it;
		while (i >= borneInf && tabSymb[i].code != UtilLex.numIdCourant)
			i--;
		if (i >= borneInf)
			return i;
		else
			return 0;
	}

	/**
	 * utilitaire de placement des caracteristiques d'un nouvel ident dans tabSymb
	 * 
	 * @param code UtilLex.numIdCourant de l'ident
	 * @param cat  categorie de l'ident parmi CONSTANTE, VARGLOBALE, PROC, etc.
	 * @param type ENT, BOOL ou NEUTRE
	 * @param info valeur pour une constante, ad d'exécution pour une variable, etc.
	 */
	private static void placeIdent(int code, int cat, int type, int info) {
		if (it == MAXSYMB)
			UtilLex.messErr("debordement de la table des symboles");
		it = it + 1;
		tabSymb[it] = new EltTabSymb(code, cat, type, info);
	}

	/**
	 * utilitaire d'affichage de la table des symboles
	 */
	private static void afftabSymb() {
		System.out.println("       code           categorie      type    info");
		System.out.println("      |--------------|--------------|-------|----");
		for (int i = 1; i <= it; i++) {
			if (i == bc) {
				System.out.print("bc=");
				Ecriture.ecrireInt(i, 3);
			} else if (i == it) {
				System.out.print("it=");
				Ecriture.ecrireInt(i, 3);
			} else
				Ecriture.ecrireInt(i, 6);
			if (tabSymb[i] == null)
				System.out.println(" reference NULL");
			else
				System.out.println(" " + tabSymb[i]);
		}
		System.out.println();
	}

	// VERIFICATION DE TYPE
	// ----------------------

	/**
	 * verification du type entier de l'expression en cours de compilation
	 * (arret de la compilation sinon)
	 */
	private static void verifEnt() {
		if (tCour != ENT)
			UtilLex.messErr("expression entiere attendue");
	}

	/**
	 * verification du type booleen de l'expression en cours de compilation
	 * (arret de la compilation sinon)
	 */
	private static void verifBool() {
		if (tCour != BOOL)
			UtilLex.messErr("expression booleenne attendue");
	}

	// COMPILATION SEPAREE
	// -------------------

	// Valeurs possible du vecteur de translation
	private static final int TRANSDON = 1, TRANSCODE = 2, REFEXT = 3;

	// descripteur associe a un programme objet
	private static Descripteur desc;

	/**
	 * modification du vecteur de translation associe au code produit
	 * + incrementation attribut nbTransExt du descripteur
	 * NB: effectue uniquement si c'est une reference externe ou si on compile un
	 * module
	 * 
	 * @param valeur TRANSDON, TRANSCODE ou REFEXT
	 */
	private static void modifVecteurTrans(int valeur) {
		if (valeur == REFEXT || desc.getUnite().equals("module")) {
			po.vecteurTrans(valeur);
			desc.incrNbTansExt();
		}
	}

	// À COMPLÉTER SI BESOIN
	// ---------------------

	/**
	 * initialisations A COMPLETER SI BESOIN
	 * -------------------------------------
	 */
	public static void initialisations() {

		// indices de gestion de la table des symboles
		it = 0;
		bc = 1;

		// pile des reprises pour compilation des branchements en avant
		pileRep = new TPileRep();
		// programme objet = code Mapile de l'unite en cours de compilation
		po = new ProgObjet();
		// COMPILATION SEPAREE: desripteur de l'unite en cours de compilation
		desc = new Descripteur();

		// initialisation necessaire aux attributs lexicaux
		UtilLex.initialisation();

		// initialisation du type de l'expression courante
		tCour = NEUTRE;

		// TODO si necessaire
		addrSymb = 0; 
		longVar = 0; 
		marqSinon = false;  
		nbrVarLoc = 0;
		nbrVarProc = 0;
		indexSautProcs = -1;

	}

	/**
	 * code des points de generation A COMPLETER
	 * -----------------------------------------
	 * 
	 * @param numGen : numero du point de generation a executer
	 */
	public static void pt(int numGen) {



	/**
     * RÉSUMÉ DES POINTS DE GÉNÉRATION 
	 * 
     * * INITIALISATION ET FIN 
     * case 0   : Initialisation générale (variables, table des symboles, pile de reprise).
     * case 255 : Fin de la compilation, génération des fichiers (.gen et .obj) et instruction ARRET.
	 * 
     * *  DÉCLARATIONS (Constantes et Variables) 
     * case 1-8  : Traitement des CONSTANTES (catégorie, valeur, type entier/booléen, ajout table).
     * case 9-13 : Traitement des VARIABLES (globales ou locales selon 'bc', typage, allocation d'adresses).
     * Génère l'instruction RESERVER pour allouer la mémoire.
	 * 
     * *  OPÉRATIONS ET EXPRESSIONS 
     * case 14-26 : Opérateurs logiques (OU, ET, NON), relationnels (=, <>, <, etc.) et arithmétiques (+, -, *, div).
     * case 27-28 : Contrôles de types sémantiques stricts (verifEnt, verifBool).
     * case 29    : Empilement des valeurs littérales directes (EMPILER).
     * case 30    : Chargement des valeurs depuis les variables en mémoire (CONTENUG, CONTENUL).
	 * 
     * *  INSTRUCTIONS DE BASE 
     * case 31-32 : Affectation (:=). Vérifie la mutabilité, le type, puis stocke (AFFECTERG, AFFECTERL).
     * case 33    : Opération de saisie utilisateur (LIRENT, LIREBOOL puis affectation).
     * case 34    : Opération d'affichage à l'écran (ECRENT, ECRBOOL).
	 * 
     * *  STRUCTURES DE CONTRÔLE DE FLUX 
     * case 35-37 : Conditionnelles (SI...ALORS...SINON...FSI). Gestion des sauts avant avec BSIFAUX et BINCOND.
     * case 38-39 : Boucles (TTQ...FAIRE...FAIT). Sauts conditionnels pour la sortie, saut inconditionnel pour boucler.
     * case 40-43 : Choix multiples (COND...AUT). Gestion complexe d'une pile de reprises pour chaîner les sauts.
	 * 
     * *  DÉCLARATION DES PROCÉDURES 
     * case 44    : Mise à jour du saut initial pour enjamber les procédures lors de l'exécution du Main.
     * case 45    : Création de l'en-tête (PROC et PRIVEE) dans la table et changement de contexte (bc).
     * case 46-48 : Ajout des paramètres (PARAMFIXE, PARAMMOD) dans la table et comptage.
     * case 49    : Fin de procédure. Génère RETOUR, masque les paramètres (code=-1) pour sécuriser
     * les appels ultérieurs, et supprime les variables locales de la table.
	 * 
     * *  APPELS DE PROCÉDURES (Contrôles stricts) 
     * case 51 : Début d'appel. Vérifie que l'identifiant est bien une PROC et réinitialise les compteurs.
     * case 52 : Fin d'appel. Vérifie s'il ne manque pas de paramètres et génère l'instruction APPEL.
     * case 53 : Transmission par adresse (MOD). Empile l'adresse (EMPILERADG/L) et vérifie 
     * que le type correspond ET que la procédure n'attendait pas un FIXE.
     * case 54 : Transmission par valeur (FIXE). Vérifie le type de l'expression calculée 
     * ET s'assure que la procédure n'attendait pas un MOD.
     */

		switch (numGen) {
			case 0:
				initialisations();
				break;
			//DECLARATION
			case 1:
				// On veut stocker une constante 
				categorie = CONSTANTE;
				break;
			case 2:

				 present = presentIdent(bc); 

				if ( present == 0){
						code = UtilLex.numIdCourant;

				}else{
					UtilLex.messErr("L'identificateur " + UtilLex.chaineIdent(UtilLex.numIdCourant) + " est déjà déclaré dans ce bloc");				}
				break;

			case 3:
					vCour = UtilLex.valEnt;
					tCour = ENT;
				break;
			case 4:
					vCour = UtilLex.valEnt;
					tCour = ENT; 
				break;
			case 5:
					vCour = -UtilLex.valEnt;
					tCour = ENT;
				break; 
			case 6:
					vCour = VRAI;
					tCour = BOOL;
				break; 
			case 7:
					vCour = FAUX;
					tCour = BOOL;
				break;
			case 8:
					placeIdent(code, categorie, tCour, vCour);
				
				break;
			case 9:
    			categorie = (bc == 1) ? VARGLOBALE : VARLOCALE;
    		break;

			case 10:
				tCour = ENT; 
				break;
			
			case 11:
				tCour = BOOL; 
				break;
			
			case 12:
    			present = presentIdent(bc);
    			if (present == 0) {
        		code = UtilLex.numIdCourant;
    			} else {
        			UtilLex.messErr("Identificateur déjà déclaré dans ce bloc");
    			}
    			if (bc == 1) {
        			info = addrSymb;
        			addrSymb++;
        			longVar++;
    			} else {
        			info = nbrVarLoc; // compteur séparé pour les locales
        			nbrVarLoc++;
    			}
    			placeIdent(code, categorie, tCour, info);
    		break;
			
			case 13:
    			if (bc == 1) {
        			po.produire(RESERVER);
        			po.produire(longVar);
    			} else {
        			po.produire(RESERVER);
        			po.produire(nbrVarLoc - (nbrVarProc + 2));
    			}
    		break;
			//  EXPRESSIONS
			case 14: 
					po.produire(OU);
					tCour= BOOL;
				break;
			case 15: 
					po.produire(ET);
					tCour= BOOL;
				break;
			case 16: 
					po.produire(NON);
					tCour= BOOL;
				break;
			case 17: 
					po.produire(EG);
					tCour= BOOL;
				break;
			case 18: 
					po.produire(DIFF);
					tCour= BOOL;
				break;
			case 19: 
					po.produire(SUP);
					tCour= BOOL;
				break;
			case 20: 
					po.produire(SUPEG);
					tCour= BOOL;
				break;
			case 21: 
					po.produire(INF);
					tCour= BOOL;
				break;
			case 22: 
					po.produire(INFEG);
					tCour= BOOL;
				break;
			case 23: 
					po.produire(ADD);
					tCour= ENT;
				break;
			case 24: 
					po.produire(SOUS);
					tCour= ENT;
				break;
			case 25: 
					po.produire(MUL);
					tCour= ENT;
				break;
			case 26: 
					po.produire(DIV);
					tCour= ENT;
				break;
			case 27: 
					verifEnt();
				break;
			case 28: 
					verifBool();
				break;

			case 29: 
					po.produire(EMPILER);
					po.produire(vCour);
				break;


			case 30:
                present = presentIdent(1); // Recherche en local puis global

                if (present == 0) {
                    UtilLex.messErr("Identificateur non déclaré : " + UtilLex.chaineIdent(UtilLex.numIdCourant));
                }

                tCour = tabSymb[present].type; 

                if (tabSymb[present].categorie == VARGLOBALE) {
                    po.produire(CONTENUG);
                    po.produire(tabSymb[present].info); 
                } 
                else if (tabSymb[present].categorie == CONSTANTE) {
                    po.produire(EMPILER);
                    po.produire(tabSymb[present].info); 
                } 
                else if (tabSymb[present].categorie == VARLOCALE || tabSymb[present].categorie == PARAMFIXE) {
                    // Accès local direct
                    po.produire(CONTENUL);
                    po.produire(tabSymb[present].info);
                    po.produire(0); 
                } 
                else if (tabSymb[present].categorie == PARAMMOD) {
                    // Accès local par indirection
                    po.produire(CONTENUL);
                    po.produire(tabSymb[present].info);
                    po.produire(1); 
                } 
                else {
                    UtilLex.messErr("Utilisation incorrecte de cet identificateur");
                }
                break;

			// AFFECTATION :=
            case 31:        
                // On trouve la ligne de la variable dans la table et on la mémorise
                indexAffect = presentIdent(1); 
                if (indexAffect == 0){
                    UtilLex.messErr("Identificateur non déclaré");
                }
                int catAff = tabSymb[indexAffect].categorie;
                
				if (catAff == CONSTANTE || catAff == PARAMFIXE) {
        			UtilLex.messErr("Constante ou Parametre Fixe non modifiable");
    			} 
    			else if (catAff != VARGLOBALE && catAff != VARLOCALE && catAff != PARAMMOD) {
        			UtilLex.messErr("Catégorie incorrecte pour une affectation");
    			}
    		break;

            case 32: 
                // On vérifie le type (en retournant lire dans la table)
                if (tabSymb[indexAffect].type == ENT){
                    verifEnt();
                } else if (tabSymb[indexAffect].type == BOOL){
                    verifBool();
                }
                // On récupère la catégorie et l'adresse mémoire (info)
                int catCible = tabSymb[indexAffect].categorie;
                int infoCible = tabSymb[indexAffect].info;
                if (catCible == VARGLOBALE) {
                    po.produire(AFFECTERG);
                    po.produire(infoCible);
                } 
                else if (catCible == VARLOCALE || catCible == PARAMFIXE) {
                    po.produire(AFFECTERL);
                    po.produire(infoCible);
                    po.produire(0); // 0 = accès direct
                } 
                else if (catCible == PARAMMOD) {
                    po.produire(AFFECTERL);
                    po.produire(infoCible);
                    po.produire(1); // 1 = indirection
                }
                break;

				// LECTURE
				case 33:
    				present = presentIdent(1);
    				if (present == 0) UtilLex.messErr("Identificateur inexistant");
    
    				int catLire = tabSymb[present].categorie;
    
    				if (catLire == CONSTANTE || catLire == PARAMFIXE) {
        				UtilLex.messErr("Impossible d'ecraser une constante ou un parametre fixe avec lire");
    				}

    				typeVar = tabSymb[present].type;
    				po.produire(typeVar == ENT ? LIRENT : LIREBOOL);
    
    				if (catLire == VARGLOBALE) { po.produire(AFFECTERG); po.produire(tabSymb[present].info); } 
    				else if (catLire == VARLOCALE) { po.produire(AFFECTERL); po.produire(tabSymb[present].info); po.produire(0); } 
    				else if (catLire == PARAMMOD) { po.produire(AFFECTERL); po.produire(tabSymb[present].info); po.produire(1); }
    			break;

			// ECRITURE
			case 34: 
					if (tCour == ENT){
						po.produire(ECRENT);
					}
					if (tCour == BOOL){
						po.produire(ECRBOOL);
					}
				break;

		//SI ALORS SINON FSI // -1 pour bsifaux ; -2 pour bincond //Case utiliser aussi pour TTQ et COND
			case 35:

					verifBool();
					po.produire(BSIFAUX);
					po.produire(-1);
					pileRep.empiler(po.getIpo());
				break;
			case 36: 
					po.produire(BINCOND);
					po.produire(-2);
					po.modifier(pileRep.depiler(),po.getIpo()+1); // Mise à jour du BSIFAUX du Si
					pileRep.empiler(po.getIpo()); // Empiler code du BINCOND dans pileRep
				
				break;

			case 37: 
					//Mise à jour du BSIFAUX ou du BINCOND
						po.modifier(pileRep.depiler(), po.getIpo()+1);

				break;
		// BOUCLE - TTQ 
			case 38: 
					pileRep.empiler(po.getIpo()+1); // Empiler ipo du début du TTQ

				break; 
		
			case 39: 
					indexBSIF = pileRep.depiler(); // Recupérer BSIFAUX de la PILE 
					po.produire(BINCOND); // Produire BINCOND
					po.produire(pileRep.depiler()); // Produire code vers début du ttq
					po.modifier(indexBSIF, po.getIpo()+1); //Mise à jour de BSIFAUX pour sortir du ttq
				break; 
	    // COND
			case 40: 
				pileRep.empiler(0); // Empiler 0 dans PileRep après rencontre du COND
				break; 

			case 41: 
				po.modifier(pileRep.depiler(), po.getIpo()+3); // Mise à jour du BSIFAUX 
				po.produire(BINCOND); 
				po.produire(pileRep.depiler()); // Produire BINCOND avec contenu en sommet de pileRep
				pileRep.empiler(po.getIpo());   
			break;  
			
			case 42:
				po.modifier(pileRep.depiler(), po.getIpo()+3); // MAJ du dernier BSIFAUX avant aut
				po.produire(BINCOND); 
				po.produire(-3);  // Produire BINCOND de aut sans dépiler 
				pileRep.empiler(po.getIpo()); // Empiler ipo du BINCOND de aut	
			break; 

			case 43:
					// Définition de l'adresse cible (juste après le fcond)
				int fcond = po.getIpo() + 1; 
					// Résolution du dernier saut d'échec (le dernier BSIFAUX ou le BINCOND de 'aut')
				int codeINC = pileRep.depiler(); 
					po.modifier(codeINC, fcond); 
					// Récupération de la tête de la chaîne des BINCOND (les succès des branches)
					// Cette valeur est soit l'adresse du dernier BINCOND, soit 0 si la chaîne est vide 
				int codePred = pileRep.depiler(); 
					// Traitement de la chaîne de reprises par bouclage 
				if (codePred != 0) { // On vérifie qu'il y a bien une chaîne à traiter
					int pred = po.getElt(codePred); // On lit l'adresse du maillon précédent dans po 

					while (pred != 0) {
						po.modifier(codePred, fcond); // On branche le BINCOND actuel vers la sortie fcond
						codePred = pred;              // On recule dans la chaîne
						pred = po.getElt(codePred);   // On lit le maillon suivant
					}
						
						// Mise à jour du tout premier BINCOND 
					po.modifier(codePred, fcond); 
				}
			break; 

			case 44:
    			if (indexSautProcs != -1) {
        			po.modifier(indexSautProcs, po.getIpo() + 1);
    			}
    			bc = 1;
    		break;

			case 45:
    			if (presentIdent(1) == 0) {
        			if (indexSautProcs == -1) {
            			po.produire(BINCOND);
            			po.produire(0);
            			indexSautProcs = po.getIpo();
        			}
        			placeIdent(UtilLex.numIdCourant, PROC, NEUTRE, po.getIpo() + 1);
        			majparam = it + 1;
        			placeIdent(-1, PRIVEE, NEUTRE, 0);
        			bc = it + 1;
        			nbrVarProc = 0;
    			} else {
        			UtilLex.messErr("Identificateur déjà déclaré");
    			}
    		break;

			case 46: // Paramètre FIXE
                if (presentIdent(bc) == 0) {
                    placeIdent(UtilLex.numIdCourant, PARAMFIXE, tCour, nbrVarProc);
                    nbrVarProc++;
                } else {
                    UtilLex.messErr("Parametre fixe déjà déclaré : " + UtilLex.chaineIdent(UtilLex.numIdCourant));
                }
                break;

            case 47: // Paramètre MODIFIABLE (PM)
                if (presentIdent(bc) == 0) {
                    placeIdent(UtilLex.numIdCourant, PARAMMOD, tCour, nbrVarProc);
                    nbrVarProc++;
                } else {
                    UtilLex.messErr("Parametre modifiable déjà déclaré : " + UtilLex.chaineIdent(UtilLex.numIdCourant));
                }
                break;

            case 48: // Fin de l'en-tête (juste avant les variables locales de la proc), On met à jour la ligne PRIVEE avec le nombre total de paramètres
                tabSymb[majparam].info = nbrVarProc;
                nbrVarLoc = nbrVarProc + 2; 

                break;

            case 49: // Fin de la procédure (Fermeture du bloc)
                int nbParamsProc = tabSymb[majparam].info; // On récupère le nb de paramètres
                po.produire(RETOUR);
                po.produire(nbParamsProc); 
				afftabSymb();
                //  On "masque" les paramètres en mettant leur code à -1 
                for (int i = 0; i < nbParamsProc; i++) {
                    tabSymb[bc + i].code = -1;
                }
                //  On coupe la table juste APRÈS les paramètres, pour effacer les locales 
                it = bc + nbParamsProc - 1; 
                //  On revient au bloc principal
                bc = 1;    
                addrSymb = longVar; 
                break;

		

			case 51: // vérifier que l'ident est bien une PROC
    			indexAffect = presentIdent(1);
    			if (indexAffect == 0)
        			UtilLex.messErr("Identificateur non déclaré : " + UtilLex.chaineIdent(UtilLex.numIdCourant));
    			if (tabSymb[indexAffect].categorie != PROC)
        			UtilLex.messErr(UtilLex.chaineIdent(UtilLex.numIdCourant) + " n'est pas une procedure");
				addrProcAppel = indexAffect; // On mémorise quelle procédure on appelle
    			indexParam = 0;              // On remet le compteur de paramètres à 0
    		break;

			case 52: // générer l'instruction APPEL
    			int adProc = tabSymb[indexAffect].info;
    			int nbParams = tabSymb[indexAffect + 1].info;
				// VÉRIFICATION : A-t-on donné assez de paramètres ?
				if (indexParam < nbParams) {
        			UtilLex.messErr("Pas assez de parametres pour l'appel de cette procedure.");
    			}
    			po.produire(APPEL);
    			po.produire(adProc);
    			po.produire(nbParams);
    		break;

			case 53: // empiler l'adresse d'un param effectif MOD
				int idMod = presentIdent(1);
    			if (idMod == 0) UtilLex.messErr("Variable non declaree.");

    			//VÉRIFICATIONS SÉMANTIQUES
    			if (indexParam >= tabSymb[addrProcAppel + 1].info) {
        			UtilLex.messErr("Trop de parametres fournis pour cette procedure.");
    			}
    			// Le paramètre dans la table est situé à : AdresseProc + 2 (pour sauter PROC et PRIVEE) + index
    			int paramModCourant = addrProcAppel + 2 + indexParam; 
    			if (tabSymb[paramModCourant].categorie != PARAMMOD) {
        			UtilLex.messErr("Erreur : Le parametre numero " + (indexParam + 1) + " attend un 'fixe', mais vous avez fourni un 'mod'.");
    			}
    			if (tabSymb[paramModCourant].type != tabSymb[idMod].type) {
        			UtilLex.messErr("Erreur de type pour le parametre 'mod' numero " + (indexParam + 1));
    			}
    			indexParam++; // On passe au paramètre suivant
    			// FIN DES VÉRIFICATIONS 

    			int catMod = tabSymb[idMod].categorie;
    			if (catMod == CONSTANTE || catMod == PARAMFIXE) {
                    UtilLex.messErr("Impossible de transmettre une constante ou un parametre fixe en mod");
                }
                else if (catMod == VARGLOBALE) { 
                    po.produire(EMPILERADG); 
                    po.produire(tabSymb[idMod].info); 
                } 
                else if (catMod == VARLOCALE) { 
                    po.produire(EMPILERADL); 
                    po.produire(tabSymb[idMod].info); 
                    po.produire(0); 
                } 
                else if (catMod == PARAMMOD) { 
                    po.produire(EMPILERADL); 
                    po.produire(tabSymb[idMod].info); 
                    po.produire(1); 
                } 
                else { 
                    UtilLex.messErr("Impossible de passer cet identificateur en parametre mod"); 
                }
			break;

			case 54: //  Vérification stricte d'un paramètre FIXE
    			if (indexParam >= tabSymb[addrProcAppel + 1].info) {
        			UtilLex.messErr("Trop de parametres fournis pour cette procedure.");
    			}
    			int paramFixeCourant = addrProcAppel + 2 + indexParam;
    			if (tabSymb[paramFixeCourant].categorie != PARAMFIXE) {
        			UtilLex.messErr("Erreur : Le parametre numero " + (indexParam + 1)+ " attend un 'mod', mais vous avez fourni un 'fixe'.");
    			}
    			if (tabSymb[paramFixeCourant].type != tCour) { // tCour contient le type de l'expression calculée
        			UtilLex.messErr("Erreur de type pour le parametre 'fixe' numero " + (indexParam + 1));
    			}
    			indexParam++; // On passe au paramètre suivant
    		break;
				
				 

			case 255:
				// En fin de compilation :
				// - création des fichiers contenant le code produit (exécutable et mnémonique)
				// - affichage de la table des symboles
				po.produire(ARRET);
				po.constObj();
				po.constGen();
				afftabSymb();
				break;

			default:
				System.out.println("Point de generation non prevu dans votre liste" + numGen);
				break;

		}
	}
}

# Projet Compilateur PROJET - Documentation Technique

Trinôme : Chabi, Gloria, Neyla  
Groupe : L3 Informatique - 2025-2026  
Date : 09 avril 2026  

---

##  Fonctionnalités implémentées

Le compilateur est totalement opérationnel et génère un code objet MAPILE conforme pour l'ensemble des structures du langage.

### 1. Gestion des déclarations et instructions
* Identificateurs : Prise en charge des variables globales, locales et des constantes avec vérification d'unicité dans la table des symboles.
* Typage : Contrôle sémantique strict des types (`ent` vs `bool`) lors des affectations, lectures et calculs d'expressions.
* Entrées/Sorties : Implémentation des instructions `lire` et `ecrire`.
* Structures de contrôle :
    * Alternance : `si...alors...sinon...fsi`.
    * Itération : `ttq...faire...fait`.
    * Choix multiple : `cond...aut...fcond` avec gestion dynamique des chaînes de branchements.

### 2. Compilation des procédures
* Sauts d'enjambement : Mise en place de branchements inconditionnels pour ignorer le corps des procédures lors du lancement du programme principal.
* Passage de paramètres : Gestion complète des paramètres par valeur (`fixe`) et par adresse (`mod`).
* Gestion du bloc : Allocation de l'espace mémoire pour les variables locales et nettoyage de la table des symboles lors de la fermeture d'une procédure (`RETOUR`).

### 3. Détection des erreurs sémantiques
Le compilateur assure une compilation sécurisée en stoppant la génération de code en cas de :
* Conflits de types : Tentative d'affectation d'une expression dont le type ne correspond pas à la variable cible.
* Droits d'accès : Tentative de modification d'une constante ou d'un paramètre défini comme `fixe`.
* Signature d'appel : Incohérence dans le nombre de paramètres ou dans le mode de passage (donner une valeur là où une adresse est attendue, ou inversement).
* Portée : Utilisation d'un identificateur non déclaré ou devenu inaccessible.

---

## Ce qui ne fonctionne pas
* D'après les tests effectués, l'ensemble des fonctionnalités implémentées est opérationnel.
* Aucune anomalie n'a été détectée sur les programmes respectant la syntaxe et la sémantique du langage PROJET..

---

##  Points ayant posé problème et solutions

1. Imbrication des structures complexes :
   * Problème : La gestion des adresses de sortie pour les instructions `COND` imbriquées pouvait entraîner des sauts vers de mauvaises instructions.
   * Solution : Utilisation d'une pile de reprises avec des marqueurs de chaînage, permettant de résoudre les adresses de sortie de manière isolée pour chaque niveau d'imbrication.


2. Validation des paramètres d'appel :
   * Problème : Détecter immédiatement si un paramètre passé par adresse (`mod`) est bien une variable modifiable et non une constante.
   * Solution : Ajout de contrôles dans les points de génération vérifiant la catégorie de chaque argument au moment de l'appel, assurant l'intégrité des données.

3. Nettoyage de la table des symboles :
   * Problème : Supprimer les variables locales en fin de procédure sans perdre les informations nécessaires au programme appelant.
   * Solution : Calcul précis de l'indice de troncature de la table après chaque procédure, combiné à un masquage des paramètres pour sécuriser la portée des identificateurs.
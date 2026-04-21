/*
 * Grammaire du langage PROJET
 * CMPL L3info 
 * Nathalie Girard, Veronique Masson, Laurent Perraudeau
 * 
 * Il convient d'y insérer les appels aux points de génération
 * de la forme {PtGen.pt(k);}
 * 
 * Attention : relancer ANTLR après chaque modification des 
 * points de génération, pour regénérer les analyseurs.
 */

grammar projet;

options {
	language = Java;
	k = 1;
}

@lexer::header { 
  package analyseurs ;

  import libIO.*;
  import compilateur.*;
}

@parser::header {
	package analyseurs ;   

 	import libIO.*;       
  	import compilateur.*;

	import java.io.IOException;
	import java.io.DataInputStream;
	import java.io.FileInputStream;

}

/*
 * Partie syntaxique : description de la grammaire
 * les non-terminaux doivent commencer par une minuscule
 */

@members {
// variables globales et methodes utiles à placer ici
  
}

@rulecatch {
// la directive rulecatch permet d'interrompre l'analyse à la première erreur de syntaxe
	catch (RecognitionException e) {
		reportError (e) ;
		throw e ; 
	}
}

unite:
	unitprog {PtGen.pt(255);} EOF
	| unitmodule EOF;

unitprog:
	'programme' ident ':' declarations 
	corps;


unitmodule:
	'module' ident ':' declarations;

declarations:
	partiedef? partieref? consts? vars? decprocs?;

partiedef:
	'def' ident (',' ident)* ptvg;

partieref:
	'ref' specif (',' specif)* ptvg;

specif:
	ident ('fixe' '(' type ( ',' type)* ')')? (
		'mod' '(' type ( ',' type)* ')'
	)?;

consts:
	'const'{PtGen.pt(1);} (ident{PtGen.pt(2);} '=' valeur{PtGen.pt(8);} ptvg)+;

vars:
	'var'{PtGen.pt(9);} (type ident{PtGen.pt(12);} ( ',' ident{PtGen.pt(12);})*  ptvg)+{PtGen.pt(13);};

type:
	'ent'{PtGen.pt(10);}
	| 'bool'{PtGen.pt(11);};

decprocs: (decproc ptvg)+ {PtGen.pt(44);}  ; 

decproc:
	'proc' ident {PtGen.pt(45);}      
	parfixe?                          
	parmod?                          
	{PtGen.pt(48);}                  
	consts? 
	vars?                  
	corps 
	{PtGen.pt(49);};                 

ptvg:
	';'
	|;

corps:
	'debut' instructions 'fin';

parfixe:
	'fixe' '(' pf (';' pf)* ')';

pf:
	type ident {PtGen.pt(46);} (',' ident {PtGen.pt(46);})*; 
parmod:
	'mod' '(' pm (';' pm)* ')';

pm:
	type ident {PtGen.pt(47);} (',' ident {PtGen.pt(47);})*;


instructions:
	instruction (';' instruction)*;

instruction:
	inssi
	| inscond
	| boucle
	| lecture
	| ecriture
	| affouappel
	|;

inssi:
	'si' expression {PtGen.pt(35);}  'alors' instructions ('sinon' {PtGen.pt(36);} instructions)? 'fsi'{PtGen.pt(37);};

inscond:
	'cond'{PtGen.pt(40);} expression{PtGen.pt(35);}':' instructions (
		','{PtGen.pt(41);} expression{PtGen.pt(35);} ':' instructions
	)* ('aut'{PtGen.pt(42);} instructions |) 'fcond'{PtGen.pt(43);};

boucle:
	'ttq'{PtGen.pt(38);} expression{PtGen.pt(35);} 'faire' instructions 'fait'{PtGen.pt(39);};

lecture:
	'lire' '(' ident{PtGen.pt(33);} (',' ident{PtGen.pt(33);})* ')';

ecriture:
	'ecrire' '(' expression {PtGen.pt(34);} (',' expression {PtGen.pt(34);})* ')';

affouappel:
	ident 
	( 
	  {PtGen.pt(31);} ':=' expression {PtGen.pt(32);} 
	  | 
	  {PtGen.pt(51);} 
	  (effixes (effmods)?)? 
	  {PtGen.pt(52);} 
	);

effixes:
	'(' (expression {PtGen.pt(54);} (',' expression {PtGen.pt(54);} )*)? ')';

effmods:
	'(' (ident {PtGen.pt(53);} (',' ident {PtGen.pt(53);} )*)? ')';

expression:  (exp1) ('ou' {PtGen.pt(28);} exp1 {PtGen.pt(28);} {PtGen.pt(14);})*;

exp1:
	exp2 ('et' {PtGen.pt(28);} exp2 {PtGen.pt(28);} {PtGen.pt(15);})*;

exp2:
	'non' exp2 {PtGen.pt(28);} {PtGen.pt(16);}
	| exp3 ; 

exp3:
	exp4 (
		'=' {PtGen.pt(27);} exp4 {PtGen.pt(27);} {PtGen.pt(17);}
		| '<>' {PtGen.pt(27);} exp4 {PtGen.pt(27);} {PtGen.pt(18);}
		| '>' {PtGen.pt(27);} exp4 {PtGen.pt(27);}  {PtGen.pt(19);}
		| '>=' {PtGen.pt(27);} exp4 {PtGen.pt(27);} {PtGen.pt(20);}
		| '<' {PtGen.pt(27);} exp4  {PtGen.pt(27);} {PtGen.pt(21);}
		| '<=' {PtGen.pt(27);}  exp4 {PtGen.pt(27);} {PtGen.pt(22);}
	)?;

exp4:
	exp5 ('+'{PtGen.pt(27);} exp5 {PtGen.pt(27);} {PtGen.pt(23);} 
	| '-' {PtGen.pt(27);} exp5 {PtGen.pt(27);} {PtGen.pt(24);})*;

exp5:
primaire ('*'{PtGen.pt(27);} primaire {PtGen.pt(25);} | 'div' {PtGen.pt(27);} primaire {PtGen.pt(26);})*;

primaire:
	valeur {PtGen.pt(29);}
	| ident {PtGen.pt(30);}
	| '(' expression ')';

valeur:
	nbentier {PtGen.pt(3);}
	| '+' nbentier{PtGen.pt(4);}
	| '-' nbentier {PtGen.pt(5);}
	| 'vrai'{PtGen.pt(6);}
	| 'faux'{PtGen.pt(7);};

/*
 * Partie lexicale  : cette partie ne doit pas être modifiée
 * Les unités lexicales ANTLR sont en majuscules
 */

// Attention : ANTLR n'autorise pas certains traitements sur les unites lexicales, 
// il est alors ncessaire de passer par un non-terminal intermediaire 
// exemple : pour l'unit lexicale INT, le non-terminal nbentier a du etre introduit

nbentier:
	INT { UtilLex.valEnt = $INT.int; }; // mise à jour de valEnt

ident:
	ID { UtilLex.traiterId($ID.text); }; // mise à jour de numIdCourant

// Les identifiants commencent obligatoirement par une lettre
ID: ('a' ..'z' | 'A' ..'Z') (
		'a' ..'z'
		| 'A' ..'Z'
		| '0' ..'9'
		| '_'
	)*;

INT:
	'0' ..'9'+;

// On ignore les blancs et tabulations
WS: (' ' | '\t' | '\r')+ { skip(); };

// Définition d'un unique "passage à la ligne" et comptage des numéros de lignes
RC: ('\n') {UtilLex.incrementeLigne(); skip() ;};

// Définition des commentaires : 
// Tout ce qui suit un caractère dièse sur une ligne est un commentaire
// Toute suite de caractères entre accolades est un commentaire
COMMENT:
	'#' ~('\r' | '\n')* {skip();}
	| '\{' (.)* '\}' {skip();};
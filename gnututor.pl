%Copyright 2009 Andrew Olney

%This file is part of GnuTutor.

%GnuTutor is free software: you can redistribute it and/or modify
%it under the terms of the GNU General Public License as published by
%the Free Software Foundation, either version 3 of the License, or
%(at your option) any later version.

%GnuTutor is distributed in the hope that it will be useful,
%but WITHOUT ANY WARRANTY; without even the implied warranty of
%MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
%GNU General Public License for more details.

%You should have received a copy of the GNU General Public License
%along with GnuTutor. If not, see <http://www.gnu.org/licenses/>.

%------------
% State Table
%------------

%X is the key to the hashtable, and Z is what we expect to be there
state(X,Z):-
	stateTable <- getValue(X) returns Y,
	Y = Z.

%--------------
% SESSION OVER
%--------------
%if we've summarized, we have nothing more to say
handlesac(PLAN,TEMP):-
	state(summary,true),
	nothingmoretosay(PLAN,TEMP).
	
nothingmoretosay(['bye bye'|X],X).
	
%--------------
% CONTRIBUTION
%--------------
handlesac(PLAN,TEMP):-
	state(sac,assertion),
	update_coverage,
	feedback(PLAN,T1),
	tutor_initiative(T1,TEMP).
	
%--------------
% FROZEN
%--------------	
handlesac(PLAN,TEMP):-
	state(sac,frozen),
	ok(PLAN,T1),
	tutor_initiative(T1,TEMP).
	
%--------------
% QUESTION
%--------------
handlesac(PLAN,TEMP):-
	state(sac,question),
	answer_question(PLAN,TEMP).

%-------------------
% METACOMMUNICATIVE
%-------------------
handlesac(PLAN,TEMP):-
	state(sac,metacom),
	repeat(PLAN,TEMP).

%---------------
% METACOGNITIVE
%---------------
handlesac(PLAN,TEMP):-
	state(sac,metacog),
	encourage(PLAN,TEMP).
	
answer_question(['i am sorry I can not answer that question. '|X],X).
repeat(PLAN,TEMP):-
	repeatbag(PLAN, T1),
	last_move(T1,TEMP).

last_move(PLAN,TEMP):-
	stateTable <- getValue(tutorMove) returns T2,
	insert_text(PLAN,T2).
	
insert_text([H|T],T).
	
repeatbag(['let me say that again. '|X],X).
encourage(['Just give me what you know and we will start from there. '|X],X).
ok(['ok. '|X],X).

%----------------
% FEEDBACK
%----------------
%no feedback on first move
feedback(PLAN, TEMP):-
	state(lastmove,[]),
	emptyfeedback(PLAN,TEMP).
	
feedback(PLAN,TEMP):-
	%would call a predicate to lsa based on last move
	state(lastmove,prompt),
	stringmatch(PLAN,TEMP).

feedback(PLAN,TEMP):-
	completioncosine(PLAN,TEMP).

%catch all: empty feedback
feedback([' '|X],X).

%completion(X) would be used to calculate these
stringmatch(PLAN,TEMP):-
	stateTable <- getValue(completion) returns X,
	tutor <- stringMatch(X) returns Y,
	Y = true,
	positivefeedback(PLAN,TEMP).

stringmatch(PLAN,TEMP):-
	neutralfeedback(PLAN,TEMP).

completioncosine(PLAN,TEMP):-
	stateTable <- getValue(completion) returns X,
	not( X = false ),
	tutor <- highCosine(X) returns Y,
	Y = true,
	positivefeedback(PLAN,TEMP).
	
completioncosine(PLAN,TEMP):-
	neutralfeedback(PLAN,TEMP).

positivefeedback(['Great. '|X],X).
neutralfeedback(['OK. '|X],X).
negativefeedback(['Nope. '|X],X).
emptyfeedback(X,X).

%------------------
% TUTOR INITIATIVE
%------------------
% we handle state (knowing what we've said) by asserting facts against the database

%if we haven't introduced the problem, introduce it
tutor_initiative(PLAN,TEMP):-
	state(introduction,false),
	introduce(PLAN,TEMP),
	%note that state table modifications are always at the end of the predicates, b/c they are always true
	stateTable <- setValue(introduction,true),
	stateTable <- setValue(lastmove,introduction).
	
%pump on the second move
tutor_initiative(PLAN,TEMP):-
	state(lastmove,introduction),
	pump(PLAN,TEMP),
	stateTable <- setValue(lastmove,pump).
			
%if we've covered all aspects, summarize
tutor_initiative(PLAN,TEMP):-
	state(summary,false),
	all_aspects_covered,
	summarize(PLAN,TEMP),
	stateTable <- setValue(summary,true).
	
summarize(PLAN,TEMP):-
	summary_intromarker(PLAN, T2),
	summary(T2, T3),
	summary_exitmarker(T3,TEMP).
	
summary([H|T],T):-
	tutor <- generateSummary returns H.
	
summary_intromarker(['In summary'|X],X).
summary_exitmarker(['This concludes the lesson'|X],X).


%if we haven't covered an aspect, try to
tutor_initiative(PLAN,TEMP):-
	all_aspects_not_covered,
	resolve_aspect(X),
	cover_aspect(X,PLAN,TEMP).

%--------------------
% Aspect
%--------------------

update_coverage:-
	tutor <- updateCoverage.
	
all_aspects_covered:-
	tutor <- checkAspectsCovered returns X,
	X = true.	
	
all_aspects_not_covered:-
	tutor <- checkAspectsCovered returns X,
	X = false.	

%if we don't have a current aspect, select one
resolve_aspect(X):-
	state(current_aspect,false),
	tutor <- selectAspect returns X,
	stateTable <- setValue(current_aspect,X).

resolve_aspect(X):-
	state(current_aspect,X).

%we get a list of goals (aspects) to cover from dm. now we find a plan to cover the head
cover_aspect(NUM,PLAN,TEMP):-
	state(lastmove,X),
	morespecific(Y,X),
	move(Y,NUM,PLAN,TEMP,COMP),
	cover_aspect_state_update(Y, COMP).
	
%a kludge to make sure the correct state occurs when we emit and elaborate+hint in one move
cover_aspect_state_update(elaboration, _).

cover_aspect_state_update(Y, COMP):-
	stateTable <- setValue(lastmove,Y),
	stateTable <- setValue(completion,COMP).


%----------------
% Dialogue moves
%----------------
	
%-------Domain General----------

pump(['can you say more'|X],X).
	
morespecific(hint,pump).
morespecific(prompt,hint).
morespecific(elaboration,prompt).

%this isn't technically true, but it lets us roll over in a natural way
morespecific(hint,elaboration).

	
%-------Domain Specific----------

introduce(['what is coffee'|X],X).

aspect('0',['coffee is a beverage created from the beans of the coffee plant.'|X],X).
aspect('1',['coffee was first consumed in the 9th century in ethiopia.'|X],X).

move(hint,'0',['What is coffee made of?'|X],X, 'coffee beans').
move(hint,'0',['What can you say about coffee beans?'|X],X, 'they are used to make coffee').
move(hint,'1',['What can you say about the origination of coffee?'|X],X,'9th century ethiopia').
move(hint,'1',['When and where did coffee begin?'|X],X, '9th century ethiopia').

move(prompt,'0',['Coffee is made of coffee what?'|X],X,beans).
move(prompt,'0',['To make coffee, you must first get some?'|X],X, beans).
move(prompt,'1',['Coffee was first consumed in 9th century in what place?'|X],X, ethiopia).
move(prompt,'1',['In what century was coffee first consumed?'|X],X, '9th').

%even though this is domain general, it's in this section b/c people may not want to handle elaboration
%this way, especially if there is more than one HPE cycle
move(elaboration,NUM,PLAN,TEMP,COMP):-
	%in other words we use the aspect as its own elaboration
	aspect(NUM,PLAN,T1),
	tutor <- incrementCycleCount,
	update_coverage,
	stateTable <- setValue(lastmove,elaboration),
	tutor_initiative(T1,TEMP).


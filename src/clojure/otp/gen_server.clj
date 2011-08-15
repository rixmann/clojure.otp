;; Copyright (C) 2011
;;
;; Distributed under the Eclipse Public License, the same as Clojure.
;;
;; Author: Ole Rixmann <rixmann.ole@googlemail.com>

(ns clojure.otp.gen_server
  (:use [pattern-match])
  (:use clojure.otp.util)
  (:require [clojure.otp.gen_fsm :as gen_fsm]))

(def cast-tag '$gen_server_cast$)
(def call-tag '$gen_server_call$)
(def callback-tag '$gen_server_callback$)
(def store-callback-tag '$gen_server_store$)

(def module 'clojure.otp.gen_server)
(declare handle_cast handle_call)

;;api
(defn start_link [Module & Args]
  (gen_fsm/start_link module Module Args))

(defn cast [Srv Message]
  (gen_fsm/send_event Srv [cast-tag Message]))

(defn call [Srv Message]
  (gen_fsm/receive_answer Srv [call-tag Message]))

(defn callback [Srv Message Key Fun]
  (do
    (gen_fsm/send_event *agent* [store-callback-tag Key Fun])
    (gen_fsm/send_event Srv [call-tag Message] [module callback-tag Key *agent*]))) 
   
(defn reply [Message To]
  (gen_fsm/send_event To [call-tag Message]))

(defn get_state [Srv]
  (:state (gen_fsm/get_data Srv)))


;;behavior
(defn init [Module Args]
  ['working
   {:module Module
    :state (apply (module_resolve Module 'init) Args)
    :callbacks {}}])

(defn send_hook [To Tag Event From]
  (cond
   (instance? clojure.lang.Agent To) (send To gen_fsm/handle_event Tag Event From)
   (= callback-tag (second To)) (gen_fsm/send_event_allstate (nth To 3) [callback-tag (nth To 2) Event] From)))

(defn working
  ([StateData Event From]
     (cond
      (= (first Event) store-callback-tag) [:noresponse 'working (assoc-in StateData :callbacks (keyword (second Event)) (nth Event 3))] 
      true (working StateData Event From nil)))
  ([StateData Event From Fun]
     (let [Module (StateData :module)
	   ServerState (StateData :state)
	   ReturnVector (match
			 (if Fun
			   (Fun (second Event) ServerState From)
			   (match Event
				  [Cast Message] :when (= Cast cast-tag) (handle_cast Module Message ServerState)
				  [Call Message] :when (= Call call-tag) (handle_call Module Message ServerState From)
				  _ (with-exception "Tag ist falsch" [:noresponse 'working ServerState])))
			 [A State Data] [A State (assoc StateData :state Data)]
			 [A Resp State Data] [A Resp State (assoc StateData :state Data)]
			 Val (with-exception (str "handle_cast / handle_call haben falsche rückgabe geliefert: \n  " Val) StateData))]
       ReturnVector)))

(defn handle_allstate [state_data Event state From]
  (if-let [[Tag CallbackKey Message] (if (= (first Event) callback-tag)
					  Event
					  nil)]
    (let [Callbacks (state_data :callbacks)
	  ReturnVector (working state_data [call-tag Message] From (get Callbacks CallbackKey))
	  NewCallbacks (if (get Callbacks CallbackKey)
			 (dissoc Callbacks CallbackKey)
			 Callbacks)
	  NewServerData (get ReturnVector (- (count ReturnVector) 1))]
      (assoc ReturnVector
	(- (count ReturnVector) 1)
	(assoc NewServerData :callbacks NewCallbacks)))
    [:noresponse state state_data]))
    
(defn terminate [Reason state state_data]
  (apply (module_resolve (:module state_data) 'terminate) [Reason (:state state_data)]))

;;utility

(defn- handle_cast [Module Message State]
  (match (apply (module_resolve Module 'handle_cast) [Message State])
	 [NORESP NewState] :when (= NORESP :noresponse) [:noresponse 'working NewState]
	 [STOP Reason NewState] :when (= STOP :stop) [:stop Reason NewState]))

(defn- handle_call [Module Message State From]
  (match (apply (module_resolve Module 'handle_call) [Message State From])
	 [NORESP NewState] :when (= NORESP :noresponse) [:noresponse 'working NewState]
	 [RESP Resp NewState] :when (= RESP :response) [:response Resp 'working NewState]
	 [STOP Reason Resp NewState] :when (= STOP :stop) [:stop Reason Resp NewState]
	 [STOP Reason NewState] :when (= STOP :stop) [:stop Reason NewState]))



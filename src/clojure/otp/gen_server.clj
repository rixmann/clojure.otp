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

(def module 'clojure.otp.gen_server)
(declare handle_cast handle_call)

;;api
(defn start_link [Module & Args]
  (gen_fsm/start_link module Module Args))

(defn cast [Srv Message]
  (gen_fsm/send_event Srv [cast-tag Message]))

(defn call [Srv Message]
  (gen_fsm/receive_answer Srv [call-tag Message]))

(defn get_state [Srv]
  (:state (gen_fsm/get_data Srv)))


;;behavior
(defn init [Module Args]
  ['working
   {:module Module
    :state (apply (module_resolve Module 'init) Args)}])

(defn working [StateData Event From]
  (let [Module (StateData :module)
	ServerState (StateData :state)]
    (match
     (match Event
	    [Cast Message] :when (= Cast cast-tag) (handle_cast Module Message ServerState)
	    [Call Message] :when (= Call call-tag) (handle_call Module Message ServerState From)
	    _ [:noresponse 'working ServerState])
     [A State Data] [A State (assoc StateData :state Data)]
     [A Resp State Data] [A Resp State (assoc StateData :state Data)])))
  
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



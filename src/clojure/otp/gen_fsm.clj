;; Copyright (C) 2011
;;
;; Distributed under the Eclipse Public License, the same as Clojure.
;;
;; Author: Ole Rixmann <rixmann.ole@googlemail.com>



(ns clojure.otp.gen_fsm
  ;; "This Namespace provides a gen_fsm for clojure.
  ;;  Implementing works like in Erlang/OTP, have a look at the way it is done there.
  ;;  However, your Namespace must provide two functions, 'init and 'terminate and a function for each state (with the same name as the state).
  (:use clojure.otp.util)
  (:use pattern-match)
  (:require [clojure.otp.gen_fsm.receiver :as receiver]))

(declare handle_event)

(def event_tag '$gen_fsm_event$)
(def allstate_event_tag '$gen_fsm_allstate$)

(def initializing_tag '$gen_fsm_initializing$)

(defn get_data [Fsm]
  (:state_data @Fsm))

(defn- handle_init_response [State [InitialState StateData]]
  (assoc State
    :state InitialState
    :state_data StateData))

(defn start_link [Namespace0 & Args]
  (let [Namespace (str Namespace0)
	Ref (agent {:module Namespace :init_args Args :state initializing_tag})]
      (send Ref (fn [State] (handle_init_response State
						  (apply (module_resolve Namespace 'init) Args))))
      Ref))

(defmacro with_state_values [State & body]
  `(let [{:keys ~'[state state_data module]} ~State]
     ~@body))


(defn send_event
  ([Fsm Event]
     (send_event Fsm Event event_tag))
  ([Fsm Event Tag]
     (if (not (nil? Fsm))
       (send Fsm
	     handle_event
	     Tag
	     Event
	     *agent*)
       :fsm_not_found)))

(defn send_event_allstate [Fsm Event]
  (send_event Fsm Event allstate_event_tag))

(defn- handle_event [State Tag Event From]
  (with_state_values
   State
   (let [handler-fn (fn [module-callback Params]
		      (if-let [EventFn (module_resolve module module-callback)]
			(match (apply EventFn Params)
			       [NORESPONSE NextState NextStateData] :when (= :noresponse NORESPONSE) (assoc State :state NextState :state_data NextStateData)
			       
			       [RESPONSE Response NextState NextStateData] :when (= :response RESPONSE) (do
													  (send_event From Response)
													  (assoc State :state NextState :state_data NextStateData))
			       [STOP Reason Response StateData] :when (= :stop STOP) (do
										       (send_event From Response)
										     (apply (module_resolve module 'terminate) [Reason state StateData]))
			       [STOP Reason StateData] :when (= :stop STOP) (apply (module_resolve module 'terminate) [Reason state StateData])
			       Val (with-exception (str "Module-callback returns wrong value.\nValue: " Val) State))
			(with-exception "Event Funktion nicht gefunden" State)))]
     (condp = Tag
	 event_tag (handler-fn state [state_data Event From])
	 allstate_event_tag (handler-fn 'handle_allstate [state_data Event state From])
	 Tag (with-exception (str "Tag stimmt nicht: " allstate_event_tag " =/= " Tag " =/= " event_tag) State)))))
  
(defn receive_answer
  ([Fsm Event] (receive_answer Fsm Event false))
  ([Fsm Event Allstate]
     (let [ag (receiver/start_link Fsm Event Allstate)]
       (loop []
	 (if (:state @ag)
	   (recur)
	   @ag)))))

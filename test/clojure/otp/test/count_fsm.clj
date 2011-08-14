;; Copyright (C) 2011
;;
;; Distributed under the Eclipse Public License, the same as Clojure.
;;
;; Author: Ole Rixmann <rixmann.ole@googlemail.com>

(ns clojure.otp.test.count_fsm
  (:use [pattern-match])
  (:require [clojure.otp.gen_fsm :as gen_fsm]))

(def module 'clojure.otp.test.count_fsm)

;;api
(defn start_link []
  (gen_fsm/start_link module))

(defn stop [Fsm]
  (gen_fsm/receive_answer Fsm :stop))

(defn increase [Fsm]
  (gen_fsm/receive_answer Fsm "inc-and-respond"))

(defn increase_noresp [Fsm]
  (gen_fsm/send_event Fsm "inc-and-re"))

(defn get_count [Fsm]
  (gen_fsm/get_data Fsm))


;;behavior
(defn init [] ['counting 0])

(defn counting [StateData Event From]
  (match Event
	 A :when (= A :stop) [:stop :ok :stopping StateData]
	 A :when (= A "inc-and-respond") [:response (+ StateData 1) 'counting (+ StateData 1)]
	 _ [:noresponse 'counting (+ StateData 1)]))

(defn terminate [Reason state state_data]
  :terminated)

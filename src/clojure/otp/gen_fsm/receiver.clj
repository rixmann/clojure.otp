;; Copyright (C) 2011
;;
;; Distributed under the Eclipse Public License, the same as Clojure.
;;
;; Author: Ole Rixmann <rixmann.ole@googlemail.com>

(ns clojure.otp.gen_fsm.receiver
;;  (:require [clojure.otp.gen_fsm :as gen_fsm])
  (:use pattern-match))

(defn start_link
  ([Fsm Event]
     (start_link Fsm Event false))
  ([Fsm Event AllState]
     (apply (resolve 'clojure.otp.gen_fsm/start_link) ['clojure.otp.gen_fsm.receiver Fsm Event AllState])))

(defn init [Fsm Event Allstate] (do
				  ((if Allstate
				     (resolve 'clojure.otp.gen_fsm/send_event_allstate)
				     (resolve 'clojure.otp.gen_fsm/send_event))
				        Fsm Event)
				  ;;			 (gen_fsm/send_event Fsm Event)
				  ['requesting "nothing"]))

(defn requesting [_ Response _]
  [:stop :normal Response])

(defn terminate [Reason State StateData]
  StateData)

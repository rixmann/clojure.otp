;; Copyright (C) 2011
;;
;; Distributed under the Eclipse Public License, the same as Clojure.
;;
;; Author: Ole Rixmann <rixmann.ole@googlemail.com>

(ns clojure.otp.test.core
  (:require [clojure.otp [gen_fsm :as gen_fsm]] :reload)
  (:require [clojure.otp.test
	     [count_fsm :as count_fsm]
	     [random_server :as random_server]] :reload)
  (:use [clojure.test]))

(deftest gen_fsm 
  (let [Counter (count_fsm/start_link)]
    (is (= 10000
	   (reduce (fn [_ _] (count_fsm/increase Counter)) 0 (range 10000)))
	"Serial sending failed")
    (reduce (fn[_ _] nil) (pmap (fn[_] (count_fsm/increase_noresp Counter)) (range 10000)))
    (Thread/sleep 1000)
    (is (= 20000 (count_fsm/get_count Counter))
	"Parallel sending failed")
    (is (= :stopping (count_fsm/stop Counter))
	"Fsm not shutting down as expected")
    (is (= :terminated @Counter)
	"Fsm not terminated")))

(deftest gen_server
  (let [rs (random_server/start_link)]
    (is (= 'working (:state @rs))
	"gen_server not started properly.")
    (random_server/produce_a_random rs)
    (is (integer? (random_server/get_a_random rs))
	"random_server didn't return the right type.")
    (random_server/stop rs)
    (Thread/sleep 10)
    (is (= :terminated @rs)
	"gen_server not terminated properly")))
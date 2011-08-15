(ns clojure.otp.test.random_server
  (:require [clojure.otp.gen_server :as gen_server])
  (:use pattern-match))

(def module 'clojure.otp.test.random_server)

;;api
(defn start_link []
  (gen_server/start_link module))

(defn produce_a_random [Srv]
  (gen_server/cast Srv :produce))

(defn get_a_random [Srv]
  (gen_server/call Srv :prod_and_answer))

(defn count_ping [Pinger Pinged]
  (gen_server/call Pinger [:ping Pinged]))

(defn stop [Srv]
  (gen_server/cast Srv :stop))

;;behavior

(defn init []
  [(java.util.Random.) 0])

(defn handle_cast [M [Ran Cnt]]
  (if (= :produce M)
    (do
      (.nextInt Ran)
      [:noresponse [Ran Cnt]])
    [:stop :terminated [Ran Cnt]]))

(defn handle_call [Message [Ran Cnt] From]
  (if (= Message :prod_and_answer)
    [:response (.nextInt Ran) [Ran Cnt]]
    [:noresponse [Ran Cnt]]))

(defn terminate [Reason State]
  Reason)

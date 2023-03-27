(ns user
  (:require [integrant.core :as ig]
            [integrant.repl :as ir]
            [integrant.repl.state :as irs]))

(defn- prep
  []
  (ig/prep ((requiring-resolve 'daveduthie.load-shedding-calendar/system))))

(ir/set-prep! prep)

(defn stop [] (ir/halt))

(defn go [] (ir/go))

(defn system [] irs/system)

(comment
  (ir/reset))

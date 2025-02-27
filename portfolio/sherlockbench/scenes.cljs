(ns sherlockbench.scenes
  (:require [portfolio.replicant :refer-macros [defscene]]
            [portfolio.ui :as portfolio]
            [sherlockbench.ui :as ui]
            [replicant.dom :refer [set-dispatch!]]))

(def pass (constantly nil))

;; stub so it won't error
(set-dispatch! pass)

(defscene text-input-field
  (ui/render-input-field "string" 0))

(defscene number-input-field
  (ui/render-input-field "integer" 0))

(defscene boolean-input-field
  (ui/render-input-field "boolean" 0))

(defscene input-form
  (ui/render-input-form ["integer" "string" "boolean"]))

(defscene show-log
  (ui/render-log-content '([:p "Hello"] [:p "do you"] [:p "like cats?"])))

(defscene buttons-investigating
  (ui/control-buttons "748d4792-b63b-40c5-bb51-cab946bd3d30" :investigate))

(defscene buttons-verifying
  (ui/control-buttons "748d4792-b63b-40c5-bb51-cab946bd3d30" :verify))

(defn main []
  (portfolio/start!
   {:config
    {:css-paths ["/styles.css"]
     :viewport/defaults
     {:background/background-color "#fdeddd"}}}))

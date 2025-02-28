(ns sherlockbench.logic
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.pprint :refer [pprint]]
            [hodgepodge.core :refer [local-storage clear!]]
            [cljs.core.async :refer [<!]]
            [reitit.frontend.easy :as reitit-easy])
  )

;; Functions in this file will be called from core.cljs.
;; Usually from the global event handler: https://replicant.fun/event-handlers/

(defn process-attempts 
  "Processes attempt data by adding problem name and initial state"
  [attempts]
  (map #(assoc %1 
               :problem-name (str "Problem " %2)
               :state :investigate)
       attempts (range 1 js/Infinity)))

(defn valid-run?
  "Asynchronously checks if a given run-id references a valid run, returns a channel."
  [run-id]
  (go
    (let [response (<! (http/post "http://localhost:3000/api/is-pending-run"
                                  {:with-credentials? false
                                   :json-params {:run-id run-id}}))]
      (:response (:body response)))))

(defn start-run [store run-id]
  (go
    (let [response (<! (http/post "http://localhost:3000/api/start-run"
                                  {:with-credentials? false
                                   :json-params (cond-> {:client-id "sherlockbench-testme"}
                                                  (not (nil? run-id)) (assoc :existing-run-id run-id))}))
          {{:keys [run-id run-type benchmark-version attempts]} :body} response
          attempts-named (process-attempts attempts)
          run-data {:run-id run-id
                    :run-type run-type
                    :benchmark-version benchmark-version
                    :attempts attempts-named}]

      (prn (str "Starting " run-type " benchmark with run-id: " run-id))

      ;; update the atom
      (reset! store run-data)
      ;; update the localStorage
      (assoc! local-storage (str "run-" run-id) run-data)
      ;; redirect to the index page
      (reitit-easy/push-state :index {:run-id run-id} {})
      )))

(defn test-function [values log-store run-id attempt-id]
  (go
    (let [response (<! (http/post "http://localhost:3000/api/test-function"
                                  {:with-credentials? false
                                   :json-params {:run-id run-id
                                                 :attempt-id attempt-id
                                                 :args values}}))
          {{output :output} :body} response]
      
      (swap! log-store conj [:p output])
      ;; TODO: when will we update localstorage?
      )))

(defn find-attempt-by-id [attempts attempt-id]
  (first (filter #(= (:attempt-id %) attempt-id) attempts)))

(defn if-run-complete [store run-id]
  (let [attempts (:attempts @store)]
    (when (every? #(= true (:completed %)) attempts)
      (let [response (<! (http/post "http://localhost:3000/api/complete-run"
                                    {:with-credentials? false
                                     :json-params {:run-id run-id}}))]
        true ;; todo finish this

        )

      )

    ))

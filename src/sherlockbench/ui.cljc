(ns sherlockbench.ui)

;; This file contains pure functions which take some state and return hiccup
;; https://replicant.fun/hiccup/
;;
;; There are two special functions:
;; - the top-level function, which everything else is a child of. This is
;;   re-rendered when our atom changes
;; - optionally a data transformation function. this just adapts
;;   the "business domain data" to the right format for the UI

(defn render-cell [data]
  [:button.cell ""])

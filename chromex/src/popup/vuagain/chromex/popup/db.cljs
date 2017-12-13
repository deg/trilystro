(ns vuagain.chromex.popup.db
  (:require
   [trilib.db :as lib-db]))

(def default-db
  (assoc lib-db/default-db
         :name "VuAgain Chrome Extension"))

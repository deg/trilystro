;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.view-modal-show-exports
  (:require
   [re-frame.loggers :refer [console]]
   [sodium.core :as na]
   [sodium.re-utils :refer [<sub >evt]]
   [trilystro.firebase :as fb] ;; [TODO] NOT NEEDED LATER

   [trilystro.fsm :as fsm]))

(defn export-lystro [{:keys [owner] :as lystro}]
  (let [;; [TODO] Move to sub
        user-details (<sub [:firebase/on-value {:path (fb/all-shared-fb-path [:user-details])}])
        user (when owner
               ((keyword owner) user-details))]
    {:uid (:firebase-id lystro)
     :visibility (if (:public lystro) "Shared" "Private")
     :owner (or (:display-name user) (:email user))
     :text (:text lystro)
     :url (:url lystro)
     :tags (vec (sort (:tags lystro)))}))


(defn modal-show-exports []
  (when (<sub [::fsm/in-page? :modal-show-exports])
    (let [lystros (<sub [::fsm/page-param])
          fn-abort  (na/>event [::fsm/goto :quit-modal])]
      [na/modal {:open? (<sub [::fsm/in-page? :modal-show-exports])
                 :dimmer "blurring"
                 :close-icon true
                 :close-on-dimmer-click? true
                 :on-close fn-abort}
       [na/modal-header {}
        (str "Exporting " (count lystros) " Lystros")
        [:div {:class "minor"}
         "Cut-n-paste to save"]]
       [na/modal-content {}
        [na/container {:class-name "literal-whitespace"}
         (with-out-str
           (cljs.pprint/pprint
            (mapv export-lystro lystros)))]]])))



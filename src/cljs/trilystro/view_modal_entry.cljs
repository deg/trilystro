;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.view-modal-entry
  (:require
   [re-frame.loggers :refer [console]]
   [soda-ash.core :as sa]
   [sodium.core :as na]
   [sodium.extensions :as nax]
   [sodium.re-utils :refer [<sub >evt]]
   [trilystro.fsm :as fsm]))


(defn entry-panel []
  (let [lystro (<sub [::fsm/page-param])
        pub? (:public? lystro)
        public? (if-not (nil? pub?)
                  pub?
                  (or (get-in (<sub [:user-settings]) [:default-public?])
                      false))]
    [na/form {:widths "equal"}
     [nax/labelled-field
      :label "Tags:"
      :inline? true
      :content [nax/tag-adder {:sub-all-tags            [:all-tags]
                               :sub-selected-tags       [::fsm/page-param-val :tags]
                               :event-set-selected-tags [::fsm/update-page-param-val :tags]}]]
     [nax/labelled-field
      :label "URL:"
      :inline? true
      :content [na/input {:type "url"
                          :placeholder "Website..."
                          :default-value (:url lystro)
                          :on-change (na/>event [::fsm/update-page-param-val :url])}]]
     [nax/labelled-field
      :label "Text:"
      :content [na/text-area {:rows 3
                              :placeholder "Description..."
                              :default-value (:text lystro)
                              :on-change (na/>event [::fsm/update-page-param-val :text])}]]
     [nax/labelled-field
      :label "Visibility:"
      :content [sa/Checkbox {:label "Public"
                             :default-checked public?
                             :on-change (na/>event [::fsm/update-page-param-val :public?] false)}]]
     (let [connected? (:firebase/connected? (<sub [:firebase/connection-state]))]
       [na/form-button {:disabled? (or (empty? (:text lystro))
                                       (not connected?))
                        :on-click (na/>event [::fsm/goto :quit-modal {:dispatch
                                                                      [:commit-lystro (assoc lystro
                                                                                             :owner (<sub [:uid])
                                                                                             :public? public?)]}])
                        :icon (if connected? "add" "wait")
                        :content (if connected?
                                   (str "Save " (if public? "public" "private"))
                                   "(offline)")
                        :positive? true}])]))


(defn modal-entry-panel []
  (let [new?  (<sub [::fsm/in-page? :modal-new-lystro])
        edit? (<sub [::fsm/in-page? :modal-edit-lystro])]
    [na/modal {:open? (or new? edit?)
               :dimmer "blurring"
               :close-icon true
               :on-close (na/>event [::fsm/goto :quit-modal])}
     [na/modal-header {} (cond new? "Add Lystro"
                               edit? "Edit Lystro"
                               :default "???")]
     [na/modal-content {} [entry-panel]]]))

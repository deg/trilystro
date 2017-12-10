;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.view-modal-entry
  (:require
   [reagent.core :as reagent]
   [re-frame.loggers :refer [console]]
   [soda-ash.core :as sa]
   [sodium.core :as na]
   [sodium.extensions :as nax]
   [iron.re-utils :refer [<sub >evt]]
   [trilib.firebase :as fb]
   [trilib.fsm :as fsm]
   [trilib.modal :as modal]))


(defn view-modal-entry-panel []
  (let [partial-tag-text (reagent/atom "")]
    (fn []
      (when (<sub [::fsm/in-page? :logged-in])
        (let [new?  (<sub [::fsm/in-page? :modal-new-lystro])
              edit? (<sub [::fsm/in-page? :modal-edit-lystro])
              lystro (when (or new? edit?)
                       (<sub [::fsm/page-param]))
              original-public? (:original-public? lystro)
              public-checked? (:public? lystro)
              public? (if (nil? public-checked?)
                        (<sub [::fb/user-settings [:default-public?] false])
                        public-checked?)]
          [modal/modal {:open? (or new? edit?)
                        :header (cond new? "Add Lystro"
                                      edit? "Edit Lystro"
                                      :default "???")}
           [na/form {:widths "equal"}
            [nax/labelled-field
             :label "Tags:"
             :inline? true
             :content [nax/tag-adder {:partial-tag-text        partial-tag-text
                                      :all-tags-sub            [:all-tags]
                                      :selected-tags-sub       [::fsm/page-param-val :tags]
                                      :set-selected-tags-event [::fsm/update-page-param-val :tags]}]]
            [nax/labelled-field
             :label "URL:"
             :inline? true
             :content [na/input {:type "url"
                                 :placeholder "Website..."
                                 :default-value (:url lystro)
                                 :on-change (na/value->event-fn [::fsm/update-page-param-val :url])}]]
            [nax/labelled-field
             :label "Text:"
             :content [na/text-area {:rows 3
                                     :placeholder "Description..."
                                     :default-value (:text lystro)
                                     :on-change (na/value->event-fn [::fsm/update-page-param-val :text])}]]
            [nax/labelled-field
             :label "Visibility:"
             :content [sa/Checkbox {:label "Public"
                                    :default-checked public?
                                    :on-change (na/value->event-fn [::fsm/update-page-param-val :public?] {:default false})}]]
            (let [connected? (:firebase/connected? (<sub [:firebase/connection-state]))]
              [na/form-button {:disabled? (or (not (empty? @partial-tag-text))
                                              (empty? (:text lystro))
                                              (not connected?))
                               :on-click #(>evt [::fsm/goto
                                                :quit-modal
                                                {:dispatch
                                                 [::fb/commit-lystro (assoc lystro
                                                                            :owner (<sub [::fb/uid])
                                                                            :original-public? original-public?
                                                                            :public? public?)]}])
                               :icon (if connected? "add" "wait")
                               :content (if connected?
                                          (str "Save " (if public? "public" "private"))
                                          "(offline)")
                               :positive? true}])]])))))

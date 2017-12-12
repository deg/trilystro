(ns vuagain.chromex.popup.views
  (:require
   [chromex.ext.runtime :as runtime :refer-macros [connect]]
   [chromex.protocols :refer [post-message!]]
   [iron.re-utils :as re-utils :refer [sub2 <sub >evt]]
   [re-frame.core :as re-frame]
   [re-frame.loggers :refer [console]]
   [reagent.core :as reagent]
   [soda-ash.core :as sa]
   [sodium.core :as na]
   [sodium.extensions :as nax]
   [trilib.firebase :as fb]
   [trilib.fsm :as fsm]))

(defn display [display?]
  {:display (if display? "block" "none")})

(defn bg-msg [message]
  (post-message! (<sub [:background-port])
                 (clj->js (assoc message :app "VuAgain"))))


(defn top-bar []
  (let [user (<sub [::fb/user])]
    [:div
     [:nav {:class "navbar navbar-default"}
      [:div {:class "container-fluid"}
       [:div {:class "navbar-header"}
        [:div {:class "navbar-brand"}]
        [:h3 "VuAgain"]]]]
     [:button.alignRight {:class "btn btn-social",
                          :type "button"
                          :id (if user "logout" "login")
                          :on-click #(bg-msg {:command (if user "sign-out" "sign-in")})}
      (if user (<sub [::fb/user-name]) "login")]]))

(defn warning-bar []
  [:p {:class "warning",
       :id "warning",
       :style {:display "none"}}])


(defn TOS-page [display?]
  [na/form {:class-name "spaPage", :id "TOSForm", :style (display display?)}
   [:h4 "VuAgain Terms of Service"]
   [:p "VuAgain is in alpha pre-release, and may not yet be fully reliable."]
   [:p "Do not yet use VuAgain to store any information that you cannot afford to\nlose, or that you must keep confidential."]
   [:p "We plan to upgrade rapidly. Please contact our"
    [:a {:href "mailto:info@vuagain.com"}
     "support desk"]" with your comments\nand suggestions."]
   [:h5 "How to Use"]
   [:p "VuAgain helps you save notes about web pages you want to remember.\nUnlike bookmarking services, VuAgain does not force\nyou to look for your saved notes. Instead, they appear in your regular\nGoogle search results."]
   [:p "You can save two kinds of notes:"]
   [:ul
    [:li "Private notes are seen only by you"]
    [:li "Public comments can be seen by everyone"]]
   [:button {:class "btn btn-default", :id "acceptTOSButton", :type "button"} "I agree"]])

(defn logged-out-page [display?]
  [:div
   [na/form {:class-name "spaPage", :id "anonymousForm", :style (display display?)}
    [:p "You do not seem to be logged in with a chrome identity."]
    [:p "VuAgain depends on features supplied by Chrome and cannot start until you are\nlogged in with a Chrome identity."]
    [:p "VuAgain will not work if you are using a non-Chrome browser or are logged in\nanonymously."]
    [:p "If you are seeing this message in other circumstances, please\ncontact our "
     [:a {:href "mailto:info@vuagain.com"} "support desk"]"."]]
   [na/form {:class-name "spaPage", :id "loginForm", :style (display display?)}
    [:p "VuAgain needs to know your Google or Facebook identity to let you share\ncomments publicly or with your friends."]]
   [TOS-page true]])


(defn logged-in-page [display?]
  (let [partial-tag-text (reagent/atom "")]
    (fn []
      (let [lystro (<sub [::fsm/page-param])
            original-public? (:original-public? lystro)
            public-checked? (:public? lystro)
            public? (if (nil? public-checked?)
                      (<sub [::fb/user-settings [:default-public?] false])
                      public-checked?)]
        [na/form {:class-name "spaPage", :id "vaForm", :style (display display?)}
         [:div {:class "form-group"}
          [:div "Add Lystro for " [:b(<sub [:title])] ", from " [:em (<sub [:url])]]
          [nax/labelled-field
           :label "Tags:"
           :inline? true
           :content [nax/tag-adder {:partial-tag-text        partial-tag-text
                                    :all-tags-sub            [::fb/all-tags]
                                    :selected-tags-sub       [::fsm/page-param-val :tags]
                                    :set-selected-tags-event [::fsm/update-page-param-val :tags]}]]
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
            [:span
             [:button {:class "btn", :id "cancelButton"} "Cancel"]
             [na/form-button {:disabled? (or (not (empty? @partial-tag-text))
                                             (empty? (:text lystro))
                                             (not connected?))
                              :on-click #(>evt [::fb/commit-lystro (assoc lystro
                                                                          :url (<sub [:url])
                                                                          :owner (<sub [::fb/uid])
                                                                          :original-public? original-public?
                                                                          :public? public?)])
                              :icon (if connected? "add" "wait")
                              :content (if connected?
                                         (str "Save " (if public? "public" "private"))
                                         "(offline)")
                              :positive? true}]])]]))))


(defn footer-bar [display?]
  [:div {:class "panel-footer", :style {:margin-top "15px"}}
   [:small
    [:p {:class "alignLeft", :id "versionString"}]
    [:p {:class "alignRight"}
     [:a {:id "showTOSButton", :href "#"} "Show Terms of Service"]]]])

(defn popup []
  (fn []
    [:div
     [top-bar]
     [:div {:class "container"}
      [warning-bar]
      (if (<sub [::fb/user])
        [logged-in-page true]
        [logged-out-page true])]
     [footer-bar true]]))

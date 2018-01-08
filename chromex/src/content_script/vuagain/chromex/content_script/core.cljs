;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017-8, David Goldfarb

(ns vuagain.chromex.content-script.core
  "VuAgain content-script support. Inject Lystros into appropriate
  locations in other web pages."
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require
   [chromex.ext.runtime :as runtime :refer-macros [connect]]
   [chromex.logging :refer-macros [log info warn error group group-end]]
   [chromex.protocols :refer [post-message!]]
   [cljs.core.async :refer [<!]]
   [clojure.string :as str]
   [dommy.core :as dommy :refer [set-attr!] :refer-macros [sel sel1]]
   [hipo.core :as hipo]
   [oops.core :as oops]
   [re-frame.loggers :refer [console]]
   [trilib.utils :refer [num-uuid pack-msg unpack-msg]]))


(def page-patterns
  "Styles of pages that we can annotate.
  Chosen by matching the regex against the page's hostname. Only the
  first match is used, so more general cases should be placed later in
  this list.
  If host-regex matches the page's hostname, then we search the page
  dom under :root, collecting all elements matching :section. Each
  such element will be annotated, if a lystro exists that matches the
  url found at the section's :match descendent element."
  [{:pattern-key :google
    :host-regex #"(?i).*.google\.co.*"
    :root "#main"
    :section ".rc"
    :match "h3 a"
    :hover? false}
   {:pattern-key :wikipedia
    :host-regex #"(?i).*\.wikipedia\.org"
    :root "ol.references"
    :section "li"
    :match "span.reference-text a"
    :hover? false}
   {:pattern-key :bare-url
    :host-regex #".*"
    :root "body"
    :section "a"
    :match nil
    :hover? true}])

(defn pattern-named [pattern-key]
  (first (filter #(= (:pattern-key %) (keyword pattern-key))
                 page-patterns)))


(defn find-element-by-vuid
  "Find dom element with matching VuAgain UUID"
  [doms vuid]
  (first (filter #(= vuid (oops/oget % "dataset.vuid"))
                 doms)))

(defn link-of [section match]
  (if (nil? match)
    section
    (sel1 section match)))


(defn annotation
  "DOM element, ready for injection, that shows one or more Lystros related
  to a URL."
  [lystros & {:keys [as-text?]}]
  ;; [TODO]
  ;; - Show creation date
  ;; - Show owner, if not me
  ;; - Show visibility (private or public)
  ;; - Nicer handling of multiple Lystros on same URL
  ;; - Think about colors
  (let [text (str "VuAgain:\n" (str/join "\n" (map :text lystros)))]
    (if as-text?
      text
      (hipo/create [:div {:style "color:black; background-color:burlyWood;"}
                    text]))))


(defn- annotate-section
  "Handle callback from background with Lystros to annotate one page section."
  [{:keys [callback-params lystros]}]
  (when-not (empty? lystros)
    (let [{:keys [vuid pattern-key]} callback-params
          {:keys [section match hover?]} (pattern-named pattern-key)
          section-elmt (find-element-by-vuid (sel section) vuid)]
      (if hover?
        (let [link-elmt (link-of section-elmt match)]
          (set-attr! link-elmt "title" (annotation lystros :as-text? true)))
        (dommy/append! section-elmt (annotation lystros))))))


(defn run-message-loop
  "Listen for messages from background."
  [message-channel]
  (go-loop []
    (when-some [message (<! message-channel)]
      (unpack-msg message
                  (fn [command message]
                    (case command
                      "got-user"    (do) ;(console :log "USER is: " (:user message))
                      "checked-url" (annotate-section message)
                      (error "Unhandled" command "message from background: " message))))
      (recur))))



(defn- check-one-URL
  "Query background for any Lystros matching a URL."
  [background-port {:keys [pattern-key match]} section]
  (let [url-elmt (link-of section match)
        url (when url-elmt (.-href url-elmt))
        vuid (num-uuid)]
    (when url
      (oops/oset! section "dataset.vuid" vuid)
      (post-message! background-port
                     (pack-msg :check-url
                               {:url url
                                :callback-params {:vuid vuid
                                                  :pattern-key pattern-key}})))))


(defn- check-each-URL
  "Query background for any Lystros matching any of the URLs on page."
  [background-port {:keys [root section] :as pattern}]
  (if-let [root (sel1 root)]
    (run! (partial check-one-URL background-port pattern)
          (sel root section))))


(defn try-one-pattern
  "Check one pattern. If appropriate, use it to parse page and query
  background for any matching Lystros."
  [background-port {:keys [host-regex] :as pattern}]
  (let [hostname (-> js/window .-location .-hostname)]
    (if (re-matches host-regex hostname)
      (check-each-URL background-port pattern)
      :no-match)))


(defn find-urls
  "Select appropriate pattern for page, and use it to query background
  for any matching Lystros.
  Note that this only runs when the page first loads. We might someday
  consider more sophisticated strategies for dynamic pages. But this
  is fine for our current primary targets: searchr results and
  Wikipedia references."
  ([background-port]
   (find-urls background-port page-patterns))
  ([background-port [pattern & more-patterns]]
   (when (= :no-match
            (try-one-pattern background-port pattern))
     ;; (This recurrence is kind of grody, but I'm not seeing a cleaner way to iterate
     ;; down the page-patterns data structure. Reduce could also work, but that feels
     ;; even grosser here, since we are running for side-effect).
     (recur background-port more-patterns))))


(defn init! []
  (let [background-port (runtime/connect)]
    (run-message-loop background-port)
    (find-urls background-port)))

(ns clj-psl.core
  "Functions for using PSL."
  (:require
   [clojure.tools.logging :as log]
   [incanter.core :as in]
   [clj-util.core :as cu]
   )
  (:import
   [edu.umd.cs.psl.application.inference MPEInference LazyMPEInference]
   [edu.umd.cs.psl.application.util GroundKernels Grounding]
   [edu.umd.cs.psl.database DataStore Database Partition]
   [edu.umd.cs.psl.database.rdbms RDBMSDataStore RDBMSUniqueStringID]
   [edu.umd.cs.psl.database.rdbms.driver H2DatabaseDriver H2DatabaseDriver$Type]
   [edu.umd.cs.psl.model.atom QueryAtom]
   [edu.umd.cs.psl.model.argument ArgumentType GroundTerm Term Variable]
   [edu.umd.cs.psl.model.formula Conjunction Disjunction Negation Rule Formula]
   [edu.umd.cs.psl.model.kernel GroundConstraintKernel ConstraintKernel]
   [edu.umd.cs.psl.model.kernel.rule ConstraintRuleKernel CompatibilityRuleKernel]
   [edu.umd.cs.psl.reasoner Reasoner ReasonerFactory]
   [edu.umd.cs.psl.util.database Queries]
   [java.util HashSet]
   ))

;;; ================== Building PSL models ===========================
         
(defn AND                
  "Return a PSL conjunction formula over given formulas."
  [& args]                                
  (Conjunction. (into-array Formula args)))
                                          
(defn OR
  "Return a PSL disjunction formula over given formulas."
  [& args]                                
  (Disjunction. (into-array Formula args)))
                                          
(defn IMPL                                
  "Return a PSL rule formula for the given body and head formulas."
  [body head]
  (Rule. body head))
                   
(defn NOT          
  "Return a PSL negation formula over the given formula."
  [formula]
  (Negation. formula))                
                     
(defmacro add-predicate 
  "Add the specified predicate to the model."
  [model pred-name argmap]
  `(do 
     (.add ~model                     ; Add the predicate to the model
           (java.util.HashMap. 
            (assoc ~argmap "predicate" (name '~pred-name)))) ; Add 'predicate'
     (defn ~pred-name [& ~'args]    ; A function returning a QueryAtom
       (QueryAtom. 
        (.getPredicate ~model (name '~pred-name)) ; Predicate from name
        (into-array                               ; Array of Terms
         Term 
         (for [~'a ~'args]
           (if (symbol? ~'a) (Variable. (name ~'a)) ~'a)))))))
                                      
(defmacro add-pred-constraint         
  "Add a predicate constraint to the model."
  [model predicate constraint-type cname]
  `(do
     (.addConstraint
      ~model
      ~constraint-type
      {"on" (.getPredicate ~model (name '~predicate))
       "name" ~cname})))
                       
(defn add-rule [model formula weight squared name]
  "Add a compatibility rule to the model."        
  (let [rk (CompatibilityRuleKernel. formula weight squared)]
    (.setName rk name)
    (.addKernel model rk)))

;;; =============== Other functions for using PSL ====================
                          
(defn close-db            
  "Close Database, catching an IllegalStateException if it is already closed."
  ;; TODO: It would be handy if this could close something that
  ;; isn't defined yet.  
  [database]
  (try
    (.close database)
    (catch IllegalStateException e (str "caught exception: " (.getMessage e)))))
                                                                               
(defn close-open-dbs "Close all open databases and returns their count."      
  [datastore]                                                            
  (let [dbs (.listOpenDatabases datastore)
        num (count dbs)]
    (doseq [d dbs] (close-db d))
    num))
        
(defn default-inference
  "Return an app for MPE inference using the configuration."
  [model database config-bundle]
  (LazyMPEInference. model database config-bundle))
                                                  
(defn default-reasoner                            
  "Return a reasoner for MPE inference using the configuration."
  [config-bundle]
  (let [r-key LazyMPEInference/REASONER_KEY
        r-def LazyMPEInference/REASONER_DEFAULT
        r-fac (.getFactory config-bundle r-key r-def)]
    (.getReasoner r-fac config-bundle)))

(defn ground-kernels-by-name
  "Return all ground kernels from a given collection with the given name."
  [ground-kernels kernel-name]
  (for [gk ground-kernels :when (= (.getName (.getKernel gk)) kernel-name)] gk))

(defn ground-kernels-by-name-sort
  "Return a list of ground kernels sorted by kernel name."
  [ground-kernels]
  (sort (fn [g1 g2]
          (let [n1 (.getName (.getKernel g1))
                n2 (.getName (.getKernel g2))]
            (compare n1 n2)))
        (seq ground-kernels)))

(defn ground-kernels-names
  "Return a list of distinct kernel names associated with the given
  collection of ground kernels."
  [ground-kernels]
  (sort (distinct (for [gk ground-kernels] (.getName (.getKernel gk))))))

(defn ground-kernels-print-summary         
  "Print a summary of ground kernels."
  ;; Without kernel name
  ([ground-kernels]
   (doseq [gk (ground-kernels-by-name-sort ground-kernels)]
     (println (str 
               (clojure.string/join (repeat 30 "="))
               (.getName (.getKernel gk))
               (clojure.string/join (repeat 30 "="))
               ))
     (if (instance? GroundConstraintKernel gk)
       (println (str "INFE: " (.getInfeasibility gk)))
       (println (str "INCO: " (.getIncompatibility gk))))
     (println (str "CLAS: " (.getSimpleName (.getClass gk))))
     (cu/printlnw (str "STRI: " gk) 100)
     (doseq [a (.getAtoms gk)]
       (println (str "ATOM: " (.getValue a) ":" a)))))
  ;; With kernel name
  ([ground-kernels kernel-name]
   (ground-kernels-print-summary
    (ground-kernels-by-name ground-kernels kernel-name))))

(defn ground-kernels-sample
  "Return a sample of the ground kernels up to a maximum size n."
  [ground-kernels n]
  (cu/random-sample-n (seq ground-kernels) n))

(defn ground-kernels-stratified-sample
  "Return a sample of ground kernels, with up to n of each kernel."
  [ground-kernels n]
  (for [kernel-name (ground-kernels-names ground-kernels)
        ground-kernel (ground-kernels-sample
                       (ground-kernels-by-name ground-kernels kernel-name)
                       n)]
    ground-kernel))

(defn kernels-info
  "Return info about kernels."
  [model]
  (for [k (.getKernels model)]
    (if (instance? ConstraintKernel k)
      {:kind "constraint" :name (.getName k)}
      {:kind "compatibility" :name (.getName k) :weight (.getWeight (.getWeight k))})))

(defn model-new
  "Return a new PSLModel associated with the given DataStore."
  [data-store]
  ;; TODO Is all the needed functionality in Model now?
  (edu.umd.cs.psl.groovy.PSLModel.
   ""                                   ; Legacy: requires some object
   data-store))

(defn mpe-inference 
  "Call mpeInference on the inference app, providing the reasoner."
  [inference-app reasoner]
  (log/info "inference:: ::starting")
  (let [result (.mpeInference inference-app reasoner)]
    (log/info "inference:: ::done")
    result))

(defn open-db
  "Return an open PSL database."
  ([datastore model parts-to-read part-to-write preds-to-close]
     {:pre [(not-any? nil? [datastore model parts-to-read part-to-write preds-to-close])]}
     (let [parts-to-read (into-array Partition parts-to-read)
           preds-to-close (HashSet.
                           (for [pname preds-to-close]
                             (.getPredicate model pname)))]
       (.getDatabase datastore part-to-write preds-to-close parts-to-read)))
  
  ;; No predicates to close
  ([datastore model parts-to-read part-to-write]
     {:pre [(not-any? nil? [datastore model parts-to-read part-to-write])]}
     (let [parts-to-read (into-array Partition parts-to-read)]
       (.getDatabase datastore part-to-write parts-to-read)))
  
  ;; No predicates to close or write partition
  ([datastore model parts-to-read]
     {:pre [(not-any? nil? [datastore model parts-to-read])]}
     (let [parts-to-read (into-array Partition parts-to-read)]
       (.getDatabase datastore (first parts-to-read) parts-to-read))))
                                                                     
(defn p                                                 
  "Get a predicate object from the PSL model"
  [model pred-name]
  (.getPredicate model pred-name))

;;; =============== Functions for handling PSL partitions ====================

(defn partition-copy-atoms
  "Copy atoms of given predicates from one partition to another"
  [datastore model part-from part-to preds]
  (let [dbr (open-db datastore model [part-from])
        dbw (open-db datastore model [part-to] part-to)]
    (try
      (doseq [pnam preds]
        (let [atoms (Queries/getAllAtoms dbr (p model pnam))]
          (doseq [atom atoms]
            (.commit dbw atom))))
      nil
      (finally (close-db dbr)
               (close-db dbw)))))
                                       
(defn partition-delete                 
  "Delete the contents of the partition in the datastore."
  [datastore partition] (.deletePartition datastore partition))
                                                              
(defn partitions-delete                                       
  "Delete the contents of the partitions in the datastore."
  
  ;; Selected partitions
  ([datastore partitions]
     (doseq [partition partitions]
       (partition-delete datastore partition))
     (count partitions))
  
  ;; All partitions
  ([datastore]
     ;; TODO: Only returns partitions for open databases..
     (let [partitions (.listPartitions datastore)]
       (doseq [partition partitions]
         (partition-delete datastore partition))
       (count partitions))))
                    
(defn partition-id-new
  "Return an integer that is (probably!) unique within this session."
  []
  ;; (let [id @partition-id-next] (swap! partition-id-next inc) id)
  ;; This is not so good, but there will be a small number of partitions
  ;; in a session and this is, for now, only intended to be used in
  ;; an interactive session
  ;; TODO rework this
  (cu/uuid-int))
       
;;; TODO this causes a problem: each time the file is loaded this is reset
;;; Use getNextPartition in datastore instead?  No that just gives the
;;; next highest according to the DB so not too safe at all.
(def partition-id-next (atom 0))

(defn partition-new
  "Return a unique partition within this session."
  []
  (Partition. (partition-id-new)))

;;; =============== Functions for handling data in PSL ====================

(defn psl-pred-append                                   
  "Add ground atoms to PSL from a dataset. The dataset's columns must
  be ordered according to the order of arguments of the predicate.
  See psl-pred-col-ordered-dataset."
  ;; Without a database
  ([datastore write-partition predicate dataset]
   (if (some #{:value} (:column-names dataset))
     ;; Insert with value
     (do
       (assert (= (last (:column-names dataset)) :value))
       (doseq [r (in/to-vect dataset)]
         (.insertValue
          (.getInserter datastore predicate write-partition)
          (last r)                       ; Value
          (to-array (drop-last 1 r))     ; Arguments
          )))
     ;; Insert without value
     (doseq [r (in/to-vect dataset)]
       (.insert
        (.getInserter datastore predicate write-partition)
        (to-array r) ; Arguments
        ))))
  ;; With a database
  ([database predicate dataset]
   (if (some #{:value} (:column-names dataset))
     ;; Insert with value
     (do
       (assert (= (last (:column-names dataset)) :value))
       (doseq [r (in/to-vect dataset)]
         (let [atom (.getAtom
                     database predicate
                     (into-array
                      GroundTerm
                      (for [t (drop-last 1 r)]
                        (RDBMSUniqueStringID. t))))]
           (.commit database (.setValue atom (last r))))
         ))
     ;; Insert without value
     (doseq [r (in/to-vect dataset)]
       (let [atom (.getAtom
                   database predicate
                   (into-array
                    GroundTerm
                    (for [t r] (RDBMSUniqueStringID. t))))]
         (.commit database (.setValue atom 1.0)))))))
          
(defn psl-pred-col-names
  "Get column names for a given predicate as a list of keywords"
  [model pred-name] 
  (vec (map keyword (vec (.getArgumentNames (.getPredicate model pred-name))))))

(defn psl-pred-col-ordered-dataset
  "Reorder columns of a dataset according to the predicate's arguments."
  [dataset model predicate-name]
  (in/sel dataset :cols
          (psl-pred-col-names model predicate-name)))

(defn psl-pred-col-sort-dataset
  "Sort the rows of a dataset by the first argument of the predicate,
  then the second, etc."
  [dataset model predicate-name]
  (in/$order
   (psl-pred-col-names model predicate-name) :asc dataset))

(defn psl-pred-read                                                            
  "Read a table from the PSL DB"
  ;; With a supplied database
  ([model db pred-name include-value]
   (let [atoms (Queries/getAllAtoms db (p model pred-name))
         col-ns (psl-pred-col-names model pred-name)]
     (if include-value 
       (in/dataset (conj col-ns :value)
                   (for [atom atoms]
                     (flatten [(for [arg (.getArguments atom)] (.toString arg))
                               (.getValue atom)])))
       (in/dataset col-ns 
                   (for [atom atoms]
                     (for [arg (.getArguments atom)] (.toString arg)))))))
  ;; Without a database
  ([model datastore parts-read pred-name include-value]
   (let [
         db (open-db
             datastore
             model
             parts-read)
         res (psl-pred-read model db pred-name include-value)]
     (close-db db)
     res)))

(defn round-atoms
  "Round atoms of open predicates using conditional probabilities, per
  Bach et al, 2015 and Goemans and D. P. Williamson., 1994."
  [database model open-predicates]
  (let [mgks (edu.umd.cs.psl.application.groundkernelstore.MemoryGroundKernelStore.)] ; For rounding re-grounding
    (log/infof "round:: ::starting")
    (log/infof "round:: reground:: ::starting")
    (Grounding/groundAll 
     model (edu.umd.cs.psl.model.atom.PersistedAtomManager. database) mgks) ; Re-ground for rounding
    (log/infof "round:: reground:: ::done")
    ;; (cu/dbg (GroundKernels/getTotalWeightedCompatibility (.getCompatibilityKernels mgks)))
    ;; (cu/dbg (GroundKernels/getExpectedTotalWeightedCompatibility (.getCompatibilityKernels mgks)))

    ;; Round random variable (open) atoms
    (doseq [rv-pred open-predicates]
      (dosync                           ; These comparisons cannot be done concurrently
       (let [;; Transform tvals to [.25,.75]
             atoms (cu/dbgtim (for [atom (cu/dbgtim (Queries/getAllAtoms database rv-pred))]
                                (let [old-val (.getValue atom)
                                      new-val (->> old-val
                                                   (* 0.5)
                                                   (+ 0.25))]
                                  (.setValue atom new-val)
                                  atom)))
             ;; Sort by descending truth value: NOT known whether this is good
             atoms (sort (comparator (fn [x y] (> (.getValue x) (.getValue y)))) 
                         atoms)]
         (log/infof "round:: ::atoms %d" (count atoms))
         ;; Starting from an arbitrary atom, greedily discretize all
         (doseq [atom atoms]
           (dosync          ; These comparisons cannot be done concurrently
            (log/infof "round:: atom:: ::starting")
            (let [val-old (.getValue atom)  ; Remember old value
                  opts                      ; Discrete options, with scores
                  (for [val-new [0.0 1.0]]  ; Possible discrete values
                    (dosync                 ; Avoid concurrency
                     (.setValue atom val-new) ; Temporarily try value
                     (let [score              ; Score for this value
                           (reduce
                            +
                            (cu/dbgtim (for [gk (.getRegisteredGroundKernels atom)]
                                         (GroundKernels/getExpectedWeightedCompatibility gk))))]
                       (.setValue atom val-old) ; Restore old value
                       {:val val-new :score score})))
                  val-best (:val (last (sort-by :score opts)))]
              (.setValue atom val-best)      ; Greedily select a value
              (.commitToDB atom))         ; Change value in DB
            (log/infof "round:: atom:: ::done")
            )) 
         ;; (cu/dbg [rv-pred (count atoms)])
         )))
    (cu/dbg (.size mgks))
    ;; (cu/dbg (GroundKernels/getTotalWeightedCompatibility (.getCompatibilityKernels mgks)))
    (.getGroundKernels mgks)            ; Return new list of ground kernels
    ))

(defn round-atoms-simple
  "Round atoms of open predicates, interpreting truth values as
  rounding probabilities."
  [database open-predicates]
  (log/infof "round:: ::starting")
  (doseq [rv-pred open-predicates]
    (let [atoms (Queries/getAllAtoms database rv-pred)]
      (log/infof "round:: ::atoms %d" (count atoms))
      (doseq [atom atoms]           
        (log/infof "round:: atom:: ::starting")
        (let [val-old (.getValue atom) ; Remember old value
              val-new (if (> (rand) val-old) 0 1)]
          (.setValue atom val-new) 
          (dosync
           (.commitToDB atom)))       ; Change value in DB
        (log/infof "round:: atom:: ::done")))))

(defn to-variables "Wrap a list as a list of Variables."
  [args]     
  (for [a args]
    (Variable. (name a))))

(defn uid "Return a Unique ID from the provided DataStore. "
  [datastore string]
  (.getUniqueID datastore string))

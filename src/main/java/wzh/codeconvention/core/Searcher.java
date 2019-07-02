package wzh.codeconvention.core;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Constituent;
import edu.stanford.nlp.trees.LabeledScoredConstituentFactory;
import edu.stanford.nlp.trees.TreeCoreAnnotations;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class Searcher {

    // Java keywords
    private static final String JAVA_KEYWORDS = "abstract assert boolean break byte case catch char class " +
            "const continue default do double else enum extends final finally float for goto if implements " +
            "import instanceof int interface long native new package private protected public return strictfp " +
            "short static super switch synchronized this throw throws transient try var void volatile while";
    private static HashSet<String> keywordSet = new HashSet<>();

    // Regular expressions
    private static final Pattern codePattern = Pattern.compile("`(.*?)`");
    private static final Pattern codeBlockEnterPattern = Pattern.compile("^`{3}.+");
    private static final Pattern tablePattern = Pattern.compile("^\\|(.*\\|)+$");
    private static final Pattern numberPattern = Pattern.compile("[0-9]+");

    // Skipped part of speech
    private static final String[] skippedPos = {
            "CC", "DT", "PRP", "PRP$", ".", ",", "`", "*", "_", "-LRB-", "-RRB-", "-LSB-", "-RSB-"
    };
    private static HashSet<String> skippedPosSet = new HashSet<>();

    // CoreNLP pipeline configuration
    private static Properties props = new Properties();
    private StanfordCoreNLP pipeline;

    // Index file
    private IndexFile file = null;

    static {
        // Add Java keywords to set
        keywordSet.addAll(Arrays.asList(JAVA_KEYWORDS.split(" ")));
        // Add skipped POS to set
        skippedPosSet.addAll(Arrays.asList(skippedPos));
        // Set up NLP pipeline
        props.setProperty("annotators", "tokenize, ssplit, pos, parse, lemma");
        props.setProperty("parse.nthreads", "4");
    }

    public static void main(String[] args) {
        var searcher = new Searcher();
        try {
            // searcher.build("google.md", "google.idx");
            searcher.load("google.idx");
            var result = searcher.search("try catch");
            result.forEach(System.out::println);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Searcher() {
        pipeline = new StanfordCoreNLP(props);
    }

    public void build(String textPath, String indexPath) throws IOException {
        // Initialize dictionary
        file = new IndexFile();
        var setDict = new HashMap<String, TreeSet<ContentTag>>();

        // Initialize reader
        var reader = new BufferedReader(new FileReader(textPath));
        // Initialize document tree
        file.root = new Node();
        Node curNode = file.root, parent = null;
        Block curBlock = null;
        // Ignore code block flag
        var cType = ContentType.PLAIN_TEXT;

        // Read file line by line
        String line;
        while ((line = reader.readLine()) != null) {
            // Skip empty line
            if (line.length() == 0) continue;
            var lineWithFeed = String.format("%s\n", line);

            // Deal with headline
            if (line.matches("^#+.*")) { // a new headline
                // Deal with document root
                if (curNode.headline == null) { // current is the first line of document
                    curNode.headline = line;
                    curNode.level = countHeadlineLevel(line);
                    continue;
                }

                // Create new node and compute headline level
                var newNode = new Node();
                newNode.headline = line;
                newNode.level = countHeadlineLevel(line);

                // Update current and parent node reference according to level
                if (newNode.level > curNode.level) { // a child encountered
                    parent = curNode;
                } else { // a parent encountered
                    int levelDiff = curNode.level - newNode.level;
                    for (var i = 0; i < levelDiff; i++)
                        curNode = curNode.parent;
                    parent = curNode.parent;
                }
                newNode.parent = parent;
                if (parent != null)
                    parent.children.add(newNode);
                curNode = newNode;
                curBlock = null;

                continue;
            }

            // Deal with code block
            var codeBlockMatcher = codeBlockEnterPattern.matcher(line);
            if (codeBlockMatcher.matches()) { // enter code block
                cType = ContentType.CODE_BLOCK;
                curBlock = new Block(cType);
                curBlock.lines.append(lineWithFeed);
                curNode.contents.add(curBlock);
                continue;
            }
            if (cType == ContentType.CODE_BLOCK) { // in code block
                if (curBlock != null)
                    curBlock.lines.append(lineWithFeed);
                if (line.equals("```")) { // to exit block
                    cType = ContentType.PLAIN_TEXT;
                    curBlock = null; // don't now what the type of next block is
                }
                continue;
            }

            // Deal with tables
            var tableMatcher = tablePattern.matcher(line);
            if (tableMatcher.matches()) { // enter table
                if (cType != ContentType.TABLE || curBlock == null) {
                    cType = ContentType.TABLE;
                    curBlock = new Block(cType);
                    curNode.contents.add(curBlock);
                }
                curBlock.lines.append(lineWithFeed);
                continue;
            }

            // Deal with plain text
            cType = ContentType.PLAIN_TEXT;
            curBlock = new Block(cType);
            curBlock.text = line.trim();
            curNode.contents.add(curBlock);
            annotate(curNode, curBlock, setDict);
        }

        // Transform set to array
        file.dict = new HashMap<>();
        setDict.forEach((var str, var set) -> file.dict.put(str, new ArrayList<>(set)));

        // Serialize the object and store as a file
        var fileOut = new FileOutputStream(indexPath);
        var objOut = new ObjectOutputStream(fileOut);
        objOut.writeObject(file);
        objOut.close();
        fileOut.close();
    }

    private void annotate(Node node, Block block, HashMap<String, TreeSet<ContentTag>> setDict) {
        String text = block.text;

        // Try to extract some java keywords
        var codeMatcher = codePattern.matcher(text);
        var matchRes = codeMatcher.results();
        matchRes.forEach((var res) -> {
            var code = res.group().replace("`", "");
            if (keywordSet.contains(code)) {
                if (!setDict.containsKey(code))
                    setDict.put(code, new TreeSet<>());
                setDict.get(code).add(new ContentTag(node, block));
            }
        });

        // Annotate text using CoreNLP
        var annotation = new Annotation(text);
        pipeline.annotate(annotation);

        for (var coreMap : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
            // Get constituent tree of this sentence
            var tree = coreMap.get(TreeCoreAnnotations.TreeAnnotation.class);
            var constituents = tree.constituents(new LabeledScoredConstituentFactory());
            var tokenList = coreMap.get(CoreAnnotations.TokensAnnotation.class);

            // Extract noun phrases needed
            var nounPhrases = new ArrayList<Constituent>();
            for (var cnst : constituents) {
                if (cnst.label().toString().equals("NP"))
                    nounPhrases.add(cnst);
            }

            // Extract lemma from noun phrases
            for (var np : nounPhrases) {
                // Remove a phrase if it contains other noun phrases
                boolean containsOther = false;
                for (var other : nounPhrases) {
                    if (np.contains(other) && np != other)
                        containsOther = true;
                }
                if (containsOther) continue;

                // Get a copy of token list
                var cnstTokenList = new ArrayList<>(tokenList.subList(np.start(), np.end() + 1));

                // Iterate all tokens and skip certain words
                for (var token : cnstTokenList) {
                    var pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                    var lemma = token.lemma().toLowerCase();

                    // Skip a token if its POS should be ignored
                    if (skippedPosSet.contains(pos)) continue;

                    // Skip a token if it contains number
                    var numberMatcher = numberPattern.matcher(lemma);
                    if (numberMatcher.matches()) continue;

                    // Convert token to its lemma
                    if (!setDict.containsKey(lemma))
                        setDict.put(lemma, new TreeSet<>());
                    setDict.get(lemma).add(new ContentTag(node, block));
                } // end token loop
            } // end noun phrases loop
        } // end sentence loop
    }

    private static int countHeadlineLevel(String headline) {
        return headline.split("#").length - 1;
    }

    public void load(String indexPath) throws IOException, ClassNotFoundException {
        var fileIn = new FileInputStream(indexPath);
        var objIn = new ObjectInputStream(fileIn);
        file = (IndexFile) objIn.readObject();
        objIn.close();
        fileIn.close();
    }

    public ArrayList<SearchResult> search(String input) throws IndexNotLoadedException {
        // Exit if indices or test is not loaded
        if (file == null) throw new IndexNotLoadedException();

        // Configure pipeline
        var props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
        props.setProperty("ssplit.isOneSentence", "true");
        var ppl = new StanfordCoreNLP(props);
        var anno = new Annotation(input);
        ppl.annotate(anno);

        // Count frequency of input keywords
        var statMap = new HashMap<ContentTag, Integer>();
        var coreMap = anno.get(CoreAnnotations.SentencesAnnotation.class).get(0);
        for (var token : coreMap.get(CoreAnnotations.TokensAnnotation.class)) {
            var lemma = token.lemma().toLowerCase();
            if (!file.dict.containsKey(lemma)) continue;
            for (var tag : file.dict.get(lemma)) {
                if (!statMap.containsKey(tag))
                    statMap.put(tag, 1);
                else
                    statMap.replace(tag, statMap.get(tag) + 1);
            }
        }

        // Convert to search result
        var result = new ArrayList<SearchResult>();
        statMap.forEach((var tag, var count) -> result.add(new SearchResult(tag, count)));
        result.sort(Comparator.comparing(SearchResult::getScore).reversed()
                .thenComparing((var res) -> res.getTag().getNode().headline.length()));

        return result;
    }

}

class IndexFile implements Serializable {
    // Parsed text
    Node root = null;
    // Keyword indices
    HashMap<String, ArrayList<ContentTag>> dict = null;
}

class IndexNotLoadedException extends Exception {
    public IndexNotLoadedException() { super("Index file is not loaded."); }
}

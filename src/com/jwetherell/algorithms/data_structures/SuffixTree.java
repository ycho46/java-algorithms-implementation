package com.jwetherell.algorithms.data_structures;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * A suffix tree is a data structure that presents the suffixes of a given string in a way that allows 
 * for a particularly fast implementation of many important string operations. 
 * 
 * http://en.wikipedia.org/wiki/Suffix_tree
 * 
 * This is adapted from http://marknelson.us/1996/08/01/suffix-trees/ which is based on the Ukkonen's algorithm.
 * 
 * @author Justin Wetherell <phishman3579@gmail.com>
 */
public class SuffixTree<C extends CharSequence> {

    private static boolean DEBUG = false;

    private String string = null;
    private char[] characters = null;

    private Map<Integer,Link> suffixLinks = new HashMap<Integer,Link>();
    private Map<Integer,Edge<C>> edgeMap = new TreeMap<Integer,Edge<C>>();

    private int currentNode = 0;
    private int firstCharIndex = 0;
    private int lastCharIndex = -1;


    public SuffixTree(C c) {
        string = c.toString();
        characters = new char[string.length()];
        for (int i=0; i<string.length(); i++) {
            characters[i] = string.charAt(i);
        }

        int length = string.length();
        for (int i=0; i<length; i++) {
            addPrefix(i);
        }
    }

    public boolean doesSubStringExist(C sub) {
        char[] chars = new char[sub.length()];
        for (int i=0; i<sub.length(); i++) {
            chars[i] = sub.charAt(i);
        }
        int[] indices = searchEdges(chars);
        int start = indices[0];
        int end = indices[1];
        int length = end-start;
        if (length == (chars.length-1)) return true;
        return false;
    }

    public List<String> getSuffixes() {
        List<String> list = getSuffixes(0);
        return list;
    }

    @SuppressWarnings("unchecked")
    private void addPrefix(int last_char_index) {
        int parent_node = 0;
        int last_parent_node = -1;

        while (true) {
            Edge<C> edge = null;
            parent_node = currentNode;
            if (isExplicit()) {
                edge = Edge.find(this,currentNode, characters[last_char_index]);
                if (edge != null) {
                    //Edge already exists
                    break;
                }
            } else { 
                //Implicit node, a little more complicated
                edge = Edge.find(this,currentNode, characters[firstCharIndex]);
                int span = lastCharIndex-firstCharIndex;
                if (characters[edge.firstCharIndex+span+1] == characters[last_char_index]) {
                    //If the edge is the last char, don't split
                    break;
                }
                parent_node = edge.split(currentNode,firstCharIndex,lastCharIndex);
            }
            edge = new Edge<C>(this,last_char_index, characters.length-1, parent_node);
            if (DEBUG) System.out.printf("Created edge to new leaf: "+edge+"\n");
            if (last_parent_node > 0) {
                //Last parent is not root, create a link.
                if (DEBUG) System.out.printf("Creating suffix link from node "+last_parent_node+" to node "+parent_node+".\n");
                suffixLinks.get(last_parent_node).suffixNode = parent_node;
            }
            last_parent_node = parent_node;
            if (currentNode == 0) {
                if (DEBUG) System.out.printf("Can't follow suffix link, I'm at the root\n");
                firstCharIndex++;
            } else {
                //Current node is not root, follow link
                if (DEBUG) System.out.printf("Following suffix link from node "+currentNode+" to node "+suffixLinks.get(currentNode).suffixNode+".\n");
                currentNode = suffixLinks.get(currentNode).suffixNode;
            }
            canonize();
        }
        if (last_parent_node > 0) {
            //Last parent is not root, create a link.
            if (DEBUG) System.out.printf("Creating suffix link from node "+last_parent_node+" to node "+parent_node+".\n");
            suffixLinks.get(last_parent_node).suffixNode = parent_node;
        }
        last_parent_node = parent_node;
        lastCharIndex++;  //Now the endpoint is the next active point
        canonize();
    };

    private boolean isExplicit() { 
        return firstCharIndex > lastCharIndex;
    }

    @SuppressWarnings("unchecked")
    private void canonize() {
        if (!isExplicit()) {
            Edge<C> edge = Edge.find(this, currentNode, characters[firstCharIndex]);
            int edge_span = edge.lastCharIndex-edge.firstCharIndex;
            while (edge_span <= (lastCharIndex-firstCharIndex)) {
                if (DEBUG) System.out.printf("Canonizing");
                firstCharIndex = firstCharIndex + edge_span + 1;
                currentNode = edge.endNode;
                if (DEBUG) System.out.printf(" "+this);
                if (firstCharIndex <= lastCharIndex) {
                    edge = Edge.find(this, edge.endNode, characters[firstCharIndex]);
                    edge_span = edge.lastCharIndex - edge.firstCharIndex;
                }
                if (DEBUG) System.out.printf(".\n");
            }
        }
    }

    public String getEdgesTable() {
        StringBuilder builder = new StringBuilder();
        if (edgeMap.size()>0) {
            int lastCharIndex = characters.length;
            builder.append("Edge\tStart\tEnd\tSuf\tfirst\tlast\tString\n"); 
            for (int key : edgeMap.keySet()) {
                Edge<C> e = edgeMap.get(key);
                Link link = suffixLinks.get(e.endNode);
                int suffix = (link!=null)?link.suffixNode:-1;
                builder.append("\t"+e.startNode+"\t"+e.endNode+"\t"+suffix+"\t"+e.firstCharIndex+"\t"+e.lastCharIndex+"\t");
                int begin = e.firstCharIndex;
                int end = (lastCharIndex < e.lastCharIndex)?lastCharIndex:e.lastCharIndex;
                builder.append(string.substring(begin, end+1));
                builder.append("\n");
            }
            builder.append("Link\tStart\tEnd\n"); 
            for (int key : suffixLinks.keySet()) {
                Link link = suffixLinks.get(key);
                builder.append("\t"+link.node+"\t"+link.suffixNode+"\n");
            }
        }
        return builder.toString();
    }

    private List<String> getSuffixes(int start) {
        List<String> list = new LinkedList<String>();
        for (int key : edgeMap.keySet()) {
            Edge<C> e = edgeMap.get(key);
            if (e == null) continue;
            if (e.startNode!=start) continue;

            String s = (string.substring(e.firstCharIndex,e.lastCharIndex+1));

            Link n = suffixLinks.get(e.endNode);
            if (n==null) {
                list.add(s);
            } else {
                List<String> list2 = getSuffixes(e.endNode);
                for (String s2 : list2) {
                    list.add(s+s2);
                }
            }
        }
        return list;
    }

    /**
     * Returns a two element int array who's 0th index is the start index and 1th is the end index.
     */
    @SuppressWarnings("unchecked")
    private int[] searchEdges(char[] query) {
        int start_node = 0;
        int qp=0; //query position
        int start_index = -1;
        int end_index = -1;
        boolean stop = false;

        while(!stop && qp < query.length){
            Edge<C> edge = Edge.find(this, start_node, query[qp]);
            if (edge == null) {
                stop=true;
                break;
            }
            if (start_node == 0) start_index = edge.firstCharIndex;
            for (int i = edge.firstCharIndex; i <=edge.lastCharIndex; i++){
                if (qp >= query.length) {
                    stop=true;
                    break;
                } else if (query[qp] == characters[i]){
                    qp++;
                    end_index = i;
                } else {
                    stop=true;
                    break;
                }
            }
            if (!stop){ //proceed with next node
                start_node = edge.endNode;
                if (start_node==-1) stop=true;
            }   
        }
        return (new int[]{start_index,end_index});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return TreePrinter.getString(this);
    }
    
    
    private static class Link {

        private int node = 0;
        private int suffixNode = -1;


        public Link(int node) {
            this.node = node;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("node=").append(node).append("\n");
            builder.append("suffix_node=").append(suffixNode).append("\n");
            return builder.toString();
        }
    };
    
    private static class Edge<C extends CharSequence> {

        private static final int KEY_MOD = 2179;  //Should be a prime that is roughly 10% larger than String
        private static int count = 1;

        private SuffixTree<C> tree = null;
        
        private int startNode = -1;
        private int endNode = 0;
        private int firstCharIndex = 0;
        private int lastCharIndex = 0;


        private Edge(SuffixTree<C> tree, int init_first, int init_last, int parent_node ) {
            this.tree = tree;
            firstCharIndex = init_first;
            lastCharIndex = init_last;
            startNode = parent_node;
            endNode = count++;
            insert(this);
        }

        private int getKey() {
            return key(startNode, tree.characters[firstCharIndex]);
        }
        
        @Override
        public int hashCode() {
            return getKey();
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj==null) return false;
            if (obj instanceof Edge) return false;
            
            @SuppressWarnings("unchecked")
            Edge<C> e = (Edge<C>)obj;
            if (startNode==e.startNode && tree.characters[firstCharIndex]==tree.characters[e.firstCharIndex]) {
                return true;
            }
            
            return false;
        }
        
        private static int key(int node, char c) {
            return ((node<<8)+c)%KEY_MOD;
        }

        private void insert(Edge<C> edge) {
            tree.edgeMap.put(edge.getKey(), edge);
        }
        
        private void remove(Edge<C> edge) {
            int i = edge.getKey();
            Edge<C> e = tree.edgeMap.remove(i);
            while (true) {
                e.startNode = -1;
                int j = i;
                while (true) {
                    i = ++i % KEY_MOD;
                    e = tree.edgeMap.get(i);
                    if (e == null) return;
                    int r = key(e.startNode, tree.characters[e.firstCharIndex]);
                    if (i >= r && r > j)
                        continue;
                    if (r > j && j > i)
                        continue;
                    if (j > i && i >= r)
                        continue;
                    break;
                }
                tree.edgeMap.put(j, e);
            }
        }
        
        @SuppressWarnings("rawtypes") 
        private static Edge find(SuffixTree tree, int node, char c) {
            int key = key(node, c);
            return ((Edge)tree.edgeMap.get(key));
        }

        private int split(int originNode, int firstCharIndex, int lastCharIndex) {
            if (DEBUG) System.out.printf("Splitting edge: "+this+"\n");
            remove(this);
            Edge<C> new_edge = new Edge<C>(tree, this.firstCharIndex, this.firstCharIndex+lastCharIndex-firstCharIndex, originNode);
            Link link = tree.suffixLinks.get(new_edge.endNode);
            if (link==null) {
                link = new Link(new_edge.endNode);
                tree.suffixLinks.put(new_edge.endNode, link);
            }
            tree.suffixLinks.get(new_edge.endNode).suffixNode = originNode;
            this.firstCharIndex += lastCharIndex - firstCharIndex + 1;
            this.startNode = new_edge.endNode;
            insert(this);
            if (DEBUG) System.out.printf("Old edge: "+this+"\n");
            if (DEBUG) System.out.printf("New edge: "+new_edge+"\n");
            return new_edge.endNode;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("start_node=").append(startNode).append("\n");
            builder.append("end_node=").append(endNode).append("\n");
            builder.append("first_char_index=").append(firstCharIndex).append("\n");
            builder.append("last_char_index=").append(lastCharIndex).append("\n");
            String s = tree.string.substring(firstCharIndex, lastCharIndex+1);
            builder.append("string=").append(s).append("\n");
            return builder.toString();
        }
    }
    
    public static class TreePrinter {
        
        public static <C extends CharSequence> void printNode(SuffixTree<C> tree) {
            System.out.println(getString(tree, null, "", true));
        }

        public static <C extends CharSequence> String getString(SuffixTree<C> tree) {
            return getString(tree, null, "", true);
        }
        
        private static <C extends CharSequence> String getString(SuffixTree<C> tree, Edge<C> e, String prefix, boolean isTail) {
            StringBuilder builder = new StringBuilder();
            int value = 0;
            if (e!=null) {
                value = e.endNode;
                String string = tree.string.substring(e.firstCharIndex, e.lastCharIndex+1);
                builder.append(prefix + (isTail ? "└── " : "├── ") + "(" + value + ") " + string + "\n");
            } else {
                builder.append(prefix + (isTail ? "└── " : "├── ") + "(" +0 + ")" + "\n");
            }
            
            if (tree.edgeMap.size()>0) {
                List<Edge<C>> children = new LinkedList<Edge<C>>();
                for (Edge<C> edge : tree.edgeMap.values()) {
                    if (edge!=null && (edge.startNode==value)) {
                        children.add(edge);
                    }
                }
                if (children != null) {
                    for (int i = 0; i < children.size() - 1; i++) {
                        Edge<C> edge = children.get(i);
                        builder.append(getString(tree, edge, prefix + (isTail ? "    " : "│   "), false));
                    }
                    if (children.size() >= 1) {
                        Edge<C> edge = children.get(children.size() - 1);
                        builder.append(getString(tree, edge, prefix + (isTail ?"    " : "│   "), true));
                    }
                }
            }
            return builder.toString();
        }
    }
}

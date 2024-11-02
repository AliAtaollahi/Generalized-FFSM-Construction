package uk.le.ac.convertFfsmToProject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;

// This class represents a directed graph
// using adjacency list representation
public class SCCFinder {

	private FastNFA<String> fsm_;
    private List<List<Integer>> sccs_;
    private Map<FastNFAState, Boolean> is_in_root_scc_;
	
	// No. of vertices
	private int V;

	// Adjacency Lists
	private LinkedList<Integer> adj[];
	private int Time;

    public SCCFinder(FastNFA<String> fsm) {
    	fsm_ = fsm;
        is_in_root_scc_ = null;
    }

    public Map<FastNFAState, Boolean> get_is_in_root_scc() {
        SCCFinder scc_finder = new SCCFinder(fsm_.getStates().size());

		for (FastNFAState state : fsm_.getStates()) {
			for (String in : fsm_.getLocalInputs(state)) {
				Collection<FastNFAState> neighbors = fsm_.getTransitions(state, in);
				for (FastNFAState neighbors_it : neighbors) 
                    scc_finder.addEdge(state.getId(), neighbors_it.getId());
			}
		}

        sccs_ = scc_finder.SCC();

        
        Integer root_scc_index = findIndexInMatrix(sccs_, (new ArrayList<>(fsm_.getInitialStates())).get(0).getId())[0];
        
        is_in_root_scc_ = fsm_.getStates().stream().collect(Collectors.toMap(s -> s, s -> false));
        
        for(FastNFAState state : fsm_.getStates()) {
            Integer state_index = findIndexInMatrix(sccs_, state.getId())[0];
            is_in_root_scc_.put(state, state_index == root_scc_index);
        }
    
        return is_in_root_scc_;
    }

	// Constructor
	private SCCFinder(int v)
	{
		V = v;
		adj = new LinkedList[v];

		for (int i = 0; i < v; ++i)
			adj[i] = new LinkedList();

		Time = 0;
	}

	// Function to add an edge into the graph
	private void addEdge(int v, int w) { adj[v].add(w); }

	// A recursive function that finds and prints strongly
	// connected components using DFS traversal
	// u --> The vertex to be visited next
	// disc[] --> Stores discovery times of visited vertices
	// low[] -- >> earliest visited vertex (the vertex with
	//			 minimum discovery time) that can be
	//			 reached from subtree rooted with current
	//			 vertex
	// st -- >> To store all the connected ancestors (could
	// be part
	//		 of SCC)
	// stackMember[] --> bit/index array for faster check
	//				 whether a node is in stack
	private void SCCUtil(int u, int low[], int disc[],
				boolean stackMember[], Stack<Integer> st)
	{

		// Initialize discovery time and low value
		disc[u] = Time;
		low[u] = Time;
		Time += 1;
		stackMember[u] = true;
		st.push(u);

		int n;

		// Go through all vertices adjacent to this
		Iterator<Integer> i = adj[u].iterator();

		while (i.hasNext()) {
			n = i.next();

			if (disc[n] == -1) {
				SCCUtil(n, low, disc, stackMember, st);

				// Check if the subtree rooted with v
				// has a connection to one of the
				// ancestors of u
				// Case 1 (per above discussion on
				// Disc and Low value)
				low[u] = Math.min(low[u], low[n]);
			}
			else if (stackMember[n] == true) {

				// Update low value of 'u' only if 'v' is
				// still in stack (i.e. it's a back edge,
				// not cross edge).
				// Case 2 (per above discussion on Disc
				// and Low value)
				low[u] = Math.min(low[u], disc[n]);
			}
		}

		// head node found, pop the stack and print an SCC
		// To store stack extracted vertices
		int w = -1;
		if (low[u] == disc[u]) {
            List<Integer> temp = new ArrayList<>();
			while (w != u) {
				w = (int)st.pop();
				temp.add(w);
				stackMember[w] = false;
			}
			sccs_.add(temp);
		}
	}

	// The function to do DFS traversal.
	// It uses SCCUtil()
	private List<List<Integer>> SCC()
	{
        sccs_ = new ArrayList<>();
		// Mark all the vertices as not visited
		// and Initialize parent and visited,
		// and ap(articulation point) arrays
		int disc[] = new int[V];
		int low[] = new int[V];
		for (int i = 0; i < V; i++) {
			disc[i] = -1;
			low[i] = -1;
		}

		boolean stackMember[] = new boolean[V];
		Stack<Integer> st = new Stack<Integer>();

		// Call the recursive helper function
		// to find articulation points
		// in DFS tree rooted with vertex 'i'
		for (int i = 0; i < V; i++) {
			if (disc[i] == -1)
				SCCUtil(i, low, disc, stackMember, st);
		}
        
        return sccs_;
	}

    public static int[] findIndexInMatrix(List<List<Integer>> matrix, int target) {
        for (int row = 0; row < matrix.size(); row++) {
            List<Integer> currentRow = matrix.get(row);
            for (int col = 0; col < currentRow.size(); col++) {
                if (currentRow.get(col) == target) {
                    return new int[]{row, col}; // Found the number, return its row and column indices
                }
            }
        }
        return null; // Number not found in the matrix
    }
}

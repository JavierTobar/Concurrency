#include <omp.h>
#include <stdlib.h>
#include <stdio.h>
#include <time.h>


struct node{
    int color;
    int vertex; // unique id
    struct node* next;
};

struct graph{
    int numVertices;
    struct node** adjLists; // stores adjacent nodes to a given node
    struct node** adjListsCopy; // we can modify this one
    struct node** listOfNodes; // stores node to be accessible by index, alternatively could've been head of adjLists
};

/**
 * Creates a node
 * @param v The label for the node
 * @return Pointer to the created node
 */
struct node* createNode(int v){
    struct node* newNode = malloc(sizeof(struct node));
    newNode->color = 0; // not needed since default is 0, but just being explicit
    newNode->next = NULL;
    newNode->vertex = v;
    return newNode;
}

/**
 * Creates a graph
 * @param vertices # of Vertices in our graph
 * @return Pointer to the created graph
 */
struct graph* createGraph(int vertices){
    struct graph* newGraph = malloc(sizeof(struct graph));
    newGraph->numVertices = vertices;
    newGraph->adjLists = malloc(vertices*sizeof(struct node*));
    newGraph->adjListsCopy = malloc(vertices*sizeof(struct node*));
    newGraph->listOfNodes = malloc(vertices*sizeof(struct node*));

    for (int i = 0; i < vertices; i++){
        newGraph->adjLists[i] = NULL;
        newGraph->adjListsCopy[i] = NULL;
        newGraph->listOfNodes[i] = createNode(i);
    }
    return newGraph;
}


/**
 * Add edge between source and dest
 * If an edge already exists then no edge is created
 * @param graph Graph that we want to add an edge in
 * @param source The source node
 * @param dest The destination node which might end up being different if an edge already existed
 * @return 0 if edge wasn't created (false) else 1 (true)
 */
int addEdge(struct graph* graph, int source, int dest) {
    // edge case (no self loops/edges allowed)
    if (source == dest)
        return 0;

    struct node* srcNode = graph->adjLists[source];
    for (int i = 0; i < graph->numVertices; i++){
        while(srcNode){
            if (srcNode->vertex == dest){
                return 0;
            }
            srcNode = srcNode->next;
        }
    }
    // We get here when there's no edge between source and dest
    // We need to add an edge from both ways in order for the program to work properly
    // Even though we're adding "2 edges", this still only counts as 1 edge from the user's POV

    // Add edge from source to dest
    struct node* newNode = createNode(dest);
    newNode->next = graph->adjLists[source];
    graph->adjLists[source] = newNode;
    newNode->next = graph->adjListsCopy[source];
    graph->adjListsCopy[source] = newNode;

    // Add edge from dest to source
    newNode = createNode(source);
    newNode->next = graph->adjLists[dest];
    graph->adjLists[dest] = newNode;
    newNode->next = graph->adjListsCopy[dest];
    graph->adjListsCopy[dest] = newNode;
    return 1;
}

/**
 * Prints the graph
 * @param graph to be printed
 */
void printGraph(struct graph* graph) {
    int v;
    printf("\n Printing the graph");
    for (v = 0; v < graph->numVertices; v++) {
        struct node* temp = graph->adjLists[v];
        printf("\n Vertex %d has an edge with the following vertices: ", v);

        while (temp) {
            printf("%d, ", temp->vertex);
            temp = temp->next;
        }
    }
    printf("\n\n");
}
/** 4->2->6->1
 * Sets color of node to lowest available color
 * NOTE: This might end up conflicting. This is ok.
 * This is why we have DetectConflicts()
 * @param adjList The adjacency list of the vertex that we want to color
 * @param vertex The vertex number that represents the vertex we want to color
 */
void setColor(struct node* adjList, struct graph* graph, int vertex){
    int color = 1;
    struct node* adjNodes = adjList;
    struct node* node = graph->listOfNodes[vertex];
//    printf("vertex current color %d \n", node->color);
    // Iterate through the adjacent list
    while(adjNodes){
//        printf("current checking for %d \n", graph->listOfNodes[adjNodes->vertex]->color);
        if (graph->listOfNodes[adjNodes->vertex]->color == color){
            color++; // check for next possible min color
            adjNodes = adjList; // reset
        } else {
            adjNodes = adjNodes->next;
        }
    }
//    printf("about to change value of node to %d \n \n", color);
    node->color = color;
}

/**
 * Partition nodes among the threads
 */
void assign(int threads, int nodes, struct node** adjLists, struct graph* graph, int firstTime) {
    // if firstTime then we treat adjLists as all the adjacent lists
    if (firstTime) {
        int partitionSize = nodes / threads;
        int carryOver = nodes % threads; // this extra work will be done by our final thread
        omp_set_num_threads(threads);
        int threadID, start, end;
#pragma omp parallel private(threadID, start, end)
        {
        threadID = omp_get_thread_num();

        printf("threadID is %d, part size is %d carry over is %d\n", threadID, partitionSize, carryOver);

        start = threadID * partitionSize; // inclusive
        if (threadID == 0)
            end = start + partitionSize; // thread 0 is special case bc it will mess up with multiplications
        else {
            end = start;
            end += (threadID == threads - 1) ? partitionSize + carryOver : partitionSize; // exclusive
        }

        printf("start is %d and end is %d \n", start, end);
#pragma omp parallel for
        for (int v = start; v < end; v++) {
            struct node *temp = adjLists[v];
//            printf("v is currently %d\n", v);
            setColor(temp, graph, v);
        }
    }} else {
        // if it's not our first time, then we know that adjacent lists contains the vertices with conflicts
        // we need to get the size of the list before we can partition
        int size = 0;
        struct node *traversal = adjLists[0];
        while (traversal) {
            size++;
            traversal = traversal->next;
        }
        int partitionSize = size / threads;
        int carryOver = size % threads; // this extra work will be done by our final thread
        omp_set_num_threads(threads);
        int threadID, start, end;
#pragma omp parallel private(threadID, start, end)
        {
            threadID = omp_get_thread_num();

            start = threadID * partitionSize; // inclusive
            end = start;
            end += (threadID == threads - 1) ? partitionSize + carryOver : partitionSize; // exclusive
#pragma omp parallel for
            for (int v = start; v < end; v++) {
                struct node *temp = adjLists[v]; // we extract the vertex # from our list of conflicts
                // with the vertex #, we extract it's adjList and call setColor accordingly
                setColor(graph->adjLists[temp->vertex], graph, temp->vertex);
            }
        }
    }
}

/**
 * Detects conflicts among the given nodes
 * @return list of vertices that had conflicts
 */
struct node** detectConflicts(int threads, int nodes, struct node** adjLists, struct graph* graph) {
    int partitionSize = nodes / threads;
    int carryOver = nodes % threads; // this extra work will be done by our final thread
    struct node **newAdjLists = malloc(nodes * sizeof(struct node *));
    for (int i = 0; i < nodes; i++)
        adjLists[i] = NULL;

    int i = 0;
    omp_set_num_threads(threads);
    int threadID, start, end;
    int foundConflict = 0;
#pragma omp parallel shared(i, foundConflict) private(threadID, start, end)
    {
        threadID = omp_get_thread_num();
        start = threadID * partitionSize; // inclusive
        end = (threadID == threads - 1) ? partitionSize * threadID + carryOver : partitionSize * threadID; // exclusive
#pragma omp parallel
        for (int v = start; v < end; v++) {
            struct node *currNode = graph->listOfNodes[v]; // aka V
            struct node *adjNodes = adjLists[v]; // aka U
            int color = currNode->color;
            while (adjNodes) {
#pragma omp critical
                {
                    if (adjNodes->color == color && adjNodes->vertex < v) {
//                newAdjLists[i++] = adjLists[v];
                        newAdjLists[i++] = currNode;
                        foundConflict = 1;
                        v++; // go to next, same as break but can't have break inside this section
                    }
                }
                adjNodes = adjNodes->next;
            }
        }
    }
    return foundConflict ? newAdjLists : NULL;
}

void colorGraph(int threads, int nodes, struct graph* graph){
    struct node** adjLists = graph->adjListsCopy;
    int firstIteration = 1;
    while(adjLists){
        assign(threads,nodes,adjLists,graph, firstIteration);
        firstIteration = 0;
        adjLists = detectConflicts(threads, nodes, adjLists, graph);
    }
}
/**
 * Finds max degree in graph (not timed)
 * @param graph
 */
void findMaxDegree(struct graph* graph){
    int v;
    int potentialMax = 0;
    int max = 0;
    for (v = 0; v < graph->numVertices; v++) {
        struct node* temp = graph->adjLists[v];
        while (temp) {
            potentialMax++;
            temp = temp->next;
        }
        max = potentialMax > max ? potentialMax : max;
        potentialMax = 0;
    }
    printf("The max degree is: %d\n", max);
}

/**
 * Finds max color in graph (not timed)
 * @param graph
 */
void findMaxColor(struct graph* graph){
    int v;
    int max = 0;
    for (v = 0; v < graph->numVertices; v++) {
        struct node* temp = graph->listOfNodes[v]; // could just check nodes, doesn't matter, not timed
            max = temp->color > max ? temp->color : max;
            temp = temp->next;
        }
    printf("The max color is: %d\n", max);
}

int main(int argc,char *argv[]) {
    // Assumes n > 3, t > 0, and 0 < e <= n(n-1)/2
    if (argc != 4) {
        printf("This program requires 3 arguments: nodes, edges, threads\n");
        return -1;
    }
    int nodes;
    int edges;
    int threads;
    nodes = atoi(argv[1]);
    edges = atoi(argv[2]);
    threads = atoi(argv[3]);
    struct graph* g = createGraph(nodes);

//    srand(time(NULL)); // seed
    srand(10);

    int i = 0;
    while (i < edges){
        // i++ if edge got created
        if(addEdge(g,rand() % nodes, rand() % nodes))
            i++;
    }
//    printGraph(g);
    // the code logic for timing in C is from https://www.geeksforgeeks.org/how-to-measure-time-taken-by-a-program-in-c/
    clock_t t;
    t = clock();
    colorGraph(threads,nodes,g);
    t = clock() - t;
    double time_taken = ((double)t)/CLOCKS_PER_SEC;
    printf("\n Coloring the graph took %f seconds \n", time_taken);
    findMaxDegree(g);
    findMaxColor(g);
    // max color varies but not by a lot.
    // this is perfectly acceptable because our algorithm is heuristic.
}


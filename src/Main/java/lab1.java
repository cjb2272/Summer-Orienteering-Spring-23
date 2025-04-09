/**
 * Description: Program discovers the optimal path for
 * orienteering across various destinations at Mendon ponds
 * park
 * Filename: lab1.java
 * Author: Charlie Baker
 * Last Edited: Spring'23
 */

package Main.java;

// imports
import javax.imageio.ImageIO; //reading terrain img
import java.awt.image.BufferedImage;
import java.awt.*; //includes Color Data type
import java.io.*;
import java.util.*;

/**
 * main class
 */
public class lab1 {

    // multipliers taken into account for distance, real world pixel size
    static double longitude = 10.29; // meters
    static double latitude = 7.55; // meters

    // terrain color values in their GRB Integer representation
    static int openLand = new Color(248, 148, 18).getRGB();
    static int roughMeadow = new Color(255, 192, 0).getRGB();
    static int openForest = new Color(255, 255, 255).getRGB(); // 'easy movement forest'
    static int slowForest = new Color(2, 208, 60).getRGB(); // 'slow run forest'
    static int denseForest = new Color(2, 136, 40).getRGB(); // 'walk forest'
    static int heavyVegetation = new Color(5, 73, 24).getRGB(); // 'impassible vegetation'
    static int water = new Color(0, 0, 255).getRGB(); // 'lake/swamp/marsh'
    static int pavedRoad = new Color(71, 51, 3).getRGB();
    static int footPath = new Color(0, 0, 0).getRGB();
    static int outOfBounds = new Color(205, 0, 101).getRGB();

    // traversed path color will be read
    static int intRGB_Red = new Color(255, 0, 0).getRGB();

    static HashMap<Integer, Double> terrainMultiplier = new HashMap<Integer, Double>();

    // the total path distance to build upon
    static Double totalOrienteeringPathDistance = 0.0;

    /**
     * terrain multipliers settled upon by making educated values,
     * based on images provided of different terrain types at mendon
     * ponds park.
     * param terrainMultiplier dictionary sourcing multipliers
     */
    public static void setTerrainMultiplier(HashMap<Integer, Double> terrainMultiplier) {
        lab1.terrainMultiplier = terrainMultiplier;
        terrainMultiplier.put(openLand, 1.1);
        terrainMultiplier.put(roughMeadow, 3.0);
        terrainMultiplier.put(openForest, 1.5);
        terrainMultiplier.put(slowForest, 2.2);
        terrainMultiplier.put(denseForest, 4.0);
        terrainMultiplier.put(heavyVegetation, 25.0);
        terrainMultiplier.put(water, 12.5);
        terrainMultiplier.put(pavedRoad, 1.0);
        terrainMultiplier.put(footPath, 1.2);
        terrainMultiplier.put(outOfBounds, 0.0); // changed from Double.POSITIVE_INFINITY to 0.0
    }

    // This is an example of how we will represent a coord with a string
    // str stringCoordinate = "345,201";
    // THIS coordtoParentPairDict will be used as the set of nodes we have taken off
    // of the frontier and calculated successors for
    public static HashMap<String, Node> coordtoParentPairDict = new HashMap<String, Node>();

    /**
     * each node/state is a pixel you can be in
     * implementation of Comparable allows priority queue to function
     * correctly
     */
    public static class Node implements Comparable<Node> {
        public Double fOFn;
        public Double gOFn;
        public Double heuristicVal;
        public String parent;
        public int xCoord;
        public int yCoord;
        public Double colorTerrainMult; // can simply be multiplier value
        public Double elevation; // needed for calulating delta z

        // totalDistanceToNode is total distance from start to this node
        // used only for outputting total path distance
        public Double totalDistanceToNode;

        public Node() {
            this.fOFn = null;
            this.gOFn = null;
            this.heuristicVal = null;
            this.colorTerrainMult = null;
            this.elevation = null;
            this.totalDistanceToNode = 0.0;
            this.parent = null;
            this.xCoord = 9999; // some value that will cause an index error
            this.yCoord = 9999; // on accidental non setting of coord vals
        }

        // Could need work, this comparable allows queue add to work
        // correctly for priority queue functionality
        @Override
        public int compareTo(Node node) {
            if (this.fOFn < node.fOFn) {
                return -1;
            } else if (this.fOFn > node.fOFn) {
                return 1;
            } else {
                return 0;
            }
        }

        public void setfOFn(Double val) {
            this.fOFn = val;
        }

        public void setHeuristicVal(Double val) {
            this.heuristicVal = val;
        }

        public void setgOFn(Double val) {
            this.gOFn = val;
        }

        public void setElevation(Double val) {
            this.elevation = val;
        }

        public void setColorTerrainMult(int pixelRGBIntVal) {
            this.colorTerrainMult = terrainMultiplier.get(pixelRGBIntVal);
        }

        public void setxCoord(int val) {
            this.xCoord = val;
        }

        public void setyCoord(int val) {
            this.yCoord = val;
        }

        public void setParent(String parentStr) {
            this.parent = parentStr;
        }

        public void setTotalDistanceToNode(Double val) {
            this.totalDistanceToNode = val;
        }

        public Double getgOFn() {
            return this.gOFn;
        }

        public Double getHeuristicVal() {
            return this.heuristicVal;
        }

        public Double getElevation() {
            return this.elevation;
        }

        public Double getColorTerrainMult() {
            return this.colorTerrainMult;
        }

        public int getxCoord() {
            return this.xCoord;
        }

        public int getyCoord() {
            return this.yCoord;
        }

        public Double getTotalDistanceToNode() {
            return this.totalDistanceToNode;
        }
    }

    /**
     * this method is not used in heuristic calculation
     * this method is only used for calculating the short distance from one
     * pixel, to its successor / neighbor
     * node elevation and coords need to be set prior
     * 
     * @param parentNode parent node object of curNode
     * @param curNode    current node
     * @return distance between the two
     */
    public static Double calculateDistance(Node parentNode, Node curNode) {
        Double deltaX, deltaY, deltaZ;
        // delta X and delta Y will always be 1 for current purpose of this method
        Double parentX = (double) parentNode.getxCoord();
        Double parentY = (double) parentNode.getyCoord();
        Double nodeX = (double) curNode.getxCoord();
        Double nodeY = (double) curNode.getyCoord();
        if (parentX > nodeX) {
            deltaX = parentX - nodeX;
        } else {
            deltaX = nodeX - parentX;
        }
        if (parentY > nodeY) {
            deltaY = parentY - nodeY;
        } else {
            deltaY = nodeY - parentY;
        }
        Double parentZ = parentNode.getElevation();
        Double curNodeZ = curNode.getElevation();
        if (parentZ > curNodeZ) {
            deltaZ = parentZ - curNodeZ;
        } else {
            deltaZ = curNodeZ - parentZ;
        }
        deltaX = deltaX * longitude;
        deltaY = deltaY * latitude;
        Double deltaXSquared = Math.pow(deltaX, 2);
        Double deltaYSquared = Math.pow(deltaY, 2);
        Double deltaZSquared = Math.pow(deltaZ, 2);
        Double sumResult = deltaXSquared + deltaYSquared + deltaZSquared;
        Double distanceResult = Math.sqrt(sumResult);
        return distanceResult;
    }

    /**
     * PRE currentNode MUST HAVE a set elevation and x/y coords
     * Quite a bit redundant given calculate distance method
     * 
     * @param currentNode must have values pre-set
     * @param targetX     goal x coord
     * @param targetY     goal y coord
     * @param targetZ     goal elevation value
     */
    public static Double calculateHeuristic(Node currentNode, Double targetX,
            Double targetY, Double targetZ) {
        Double deltaX, deltaY, deltaZ;
        Double curNodeX = (double) currentNode.getxCoord();
        Double curNodeY = (double) currentNode.getyCoord();
        if (targetX > curNodeX) {
            deltaX = targetX - curNodeX;
        } else {
            deltaX = curNodeX - targetX;
        }
        if (targetY > curNodeY) {
            deltaY = targetY - curNodeY;
        } else {
            deltaY = curNodeY - targetY;
        }
        Double curNodeElevation = currentNode.getElevation();
        if (targetZ > curNodeElevation) {
            deltaZ = targetZ - curNodeElevation;
        } else {
            deltaZ = curNodeElevation - targetZ;
        }
        deltaX = deltaX * longitude;
        deltaY = deltaY * latitude;
        Double deltaXSquared = Math.pow(deltaX, 2);
        Double deltaYSquared = Math.pow(deltaY, 2);
        Double deltaZSquared = Math.pow(deltaZ, 2);
        Double sumResult = deltaXSquared + deltaYSquared + deltaZSquared;
        Double rootResult = Math.sqrt(sumResult);
        // getting multiplier for paved road is redundant, given pavedRoad is x1.0
        Double heuristicResult = rootResult * terrainMultiplier.get(pavedRoad);
        return heuristicResult;
    }

    /**
     * calls upon calculateDistance method and incorporates the terrain factor
     * terrains mults need to be set prior
     * 
     * @param parentLeaving parent node
     * @param pixelEntering child node of parent
     * @return g of n value
     */
    public static Double calculategOFn(Node parentLeaving, Node pixelEntering) {
        Double distanceBetween = calculateDistance(parentLeaving, pixelEntering);
        Double parentTerrain = parentLeaving.getColorTerrainMult();
        Double pixelTerrain = pixelEntering.getColorTerrainMult();
        Double averageTerrain = (parentTerrain + pixelTerrain) / 2;
        Double costResult = distanceBetween * averageTerrain;
        return costResult;
    }

    /**
     * Calculate the f(n) value for a node. Node must have
     * g(n) and h(n) values already set
     * 
     * @param n the node to calulate f(n) for
     * @return f(n)
     */
    public static Double calculatefOFn(Node n) {
        return n.getgOFn() + n.getHeuristicVal();
    }

    /**
     * This method does some overpainting, the changes dictated by + - comments
     * to this method allowed the sepentine test cases to avoid infinite loop,
     * to avoid inconsistencies in the coordtoparentpairdict where two entries had
     * the same x and y coord incorrectly for some reason.
     * We now step back parent to parent a bit differently, making more direct
     * use of node's parent attribute string for painting, which DID NOT have
     * any inconsistencies/incorrectness in the dictionary
     * 
     * @param foundNode    node we are starting at for paint backtracking
     * @param rgbIntPixels 2d int array of rgb color ints
     * @return updated pixels, PATH
     */
    public static int[][] paintTraversedPath(Node foundNode, int[][] rgbIntPixels) {
        int xValue = foundNode.getxCoord();
        int yValue = foundNode.getyCoord();
        rgbIntPixels[xValue][yValue] = intRGB_Red;
        Node tempNode = foundNode;
        while (tempNode.parent != null) {
            // String tempNodeKeyStr = tempNode.getxCoord() + "," + tempNode.getyCoord();
            // //-
            String tempNodeKeyStr = xValue + "," + yValue; // +
            Node nextNode = coordtoParentPairDict.get(tempNodeKeyStr);
            if (nextNode == null) {
                break;
            } // +
            if (nextNode.parent != null) { // +
                xValue = Integer.parseInt(nextNode.parent.split(",")[0]); // +
                yValue = Integer.parseInt(nextNode.parent.split(",")[1]); // +
            }
            rgbIntPixels[xValue][yValue] = intRGB_Red; // +
            int xCoordVal = nextNode.getxCoord();
            int yCoordVal = nextNode.getyCoord();
            rgbIntPixels[xCoordVal][yCoordVal] = intRGB_Red;
            tempNode = nextNode;
        }
        return rgbIntPixels;
    }

    /**
     * ########################## RIDICULOUSLY REDUNDANT #########################
     * 
     * @param startCoord
     * @param targetCoord
     * @param rgbIntegerPixels     [395][500] integer representation of RGB value
     *                             for all pixels
     * @param coordinateElevations [395][500] pixel/node/coordinate elevations given
     * @param visitedArr           if node is parent (has generated successors) then
     *                             node is in visited
     * @param cumulativeRgbPixels  updated pixels to start pixel (Path drawn up to
     *                             this point)
     * @return updated pixels from start to target (Path drawn from start to target)
     */
    public static int[][] aStarSearch(int[][] startCoord, int[][] targetCoord,
            int[][] rgbIntegerPixels, double[][] coordinateElevations,
            int[][] visitedArr, int[][] cumulativeRgbPixels) {
        int startX = startCoord[0][0];
        int startY = startCoord[0][1];
        int targetX = targetCoord[0][0];
        int targetY = targetCoord[0][1];
        // initialize starting node:
        Node start = new Node();
        start.setxCoord(startX);
        start.setyCoord(startY);
        String startKeyStr = startX + "," + startY;
        Double startElevation = coordinateElevations[startX][startY];
        Double targetElevation = coordinateElevations[targetX][targetY];
        int startRGBInt = rgbIntegerPixels[startX][startY];
        start.setTotalDistanceToNode(0.0);// THIS WILL NEED CHANGE ON numerous a*
        start.setgOFn(0.0);
        start.setElevation(startElevation);
        start.setColorTerrainMult(startRGBInt);
        Double heuristicVal = calculateHeuristic(start, (double) targetX,
                (double) targetY, targetElevation);
        start.setHeuristicVal(heuristicVal);
        Double fOFnEstimatedCost = calculatefOFn(start);
        start.setfOFn(fOFnEstimatedCost); // now that Node start has an f(n) val ready to enter pQueue
        coordtoParentPairDict.put(startKeyStr, null); // root has no parent

        // create FRONTIER queue and push starting word into queue / insert starting
        // word
        PriorityQueue<Node> pQueue = new PriorityQueue<>();
        // add the starting pixel to the priority queue/FRONTIER, it will be the only
        // thing
        // in queue when we pop at first to generate successors
        pQueue.add(start);

        // while the frontier is not empty
        while (!pQueue.isEmpty()) {
            // remove first word from the queue, take off the frontier to generate
            // successors
            Node poppedNode = pQueue.remove(); // this will be starting node first pass
            int xVal = poppedNode.getxCoord();
            int yVal = poppedNode.getyCoord();
            String parentKeyStr = xVal + "," + yVal; // for setting successors parents
            // goal test below
            if ((xVal == targetX) && (yVal == targetY)) {
                // WE HAVE TRAVERSED A PATH TO the target node
                totalOrienteeringPathDistance = totalOrienteeringPathDistance +
                        poppedNode.getTotalDistanceToNode();
                // paint the traversed path, backtracking
                int[][] rgbIntPixelsUpdated = paintTraversedPath(poppedNode, cumulativeRgbPixels);
                return rgbIntPixelsUpdated;
            }
            // if poppedNode is already present in visited, already a parent
            if (visitedArr[xVal][yVal] == 1) {
                continue; // don't bother generating successors
            }

            // generate successors of poppedNode!
            // begin generating 1 out of 4 successors, the north pixel
            Node northPixel = new Node();
            int northX = xVal;
            int northY;
            if (yVal > 0) {
                northY = yVal - 1;
            } // out of bounds fix on north edge of terrain
            else {
                northY = 0;
            }
            // check that 1st successor is not out of bounds - matches pink color,
            int northRGBInt = rgbIntegerPixels[northX][northY];
            // if this pixel is out of bounds bc we match pink color, then ignore this pixel
            if (!(terrainMultiplier.get(northRGBInt) == 0.0)) {
                String northKeyStr = northX + "," + northY;
                // if the node we are trying to generate is NOT a parent,
                // indicated by a 0 in the visited array, then continue
                if (visitedArr[northX][northY] == 0) {
                    northPixel.setColorTerrainMult(northRGBInt);
                    northPixel.setParent(parentKeyStr);
                    northPixel.setElevation(coordinateElevations[northX][northY]);
                    northPixel.setxCoord(northX);
                    northPixel.setyCoord(northY);
                    coordtoParentPairDict.put(northKeyStr, poppedNode); // northsPixel's parent is poppedNode
                    Double northgOFn = calculategOFn(poppedNode, northPixel);
                    northPixel.setgOFn(northgOFn);
                    Double northHeuristicVal = calculateHeuristic(northPixel,
                            (double) targetX, (double) targetY, targetElevation);
                    northPixel.setHeuristicVal(northHeuristicVal);
                    Double northfOFnEstimatedCost = calculatefOFn(northPixel);
                    northPixel.setfOFn(northfOFnEstimatedCost);
                    Double totalDistanceToNorth = poppedNode.getTotalDistanceToNode() +
                            calculateDistance(poppedNode, northPixel);
                    northPixel.setTotalDistanceToNode(totalDistanceToNorth);
                    // NOW THAT WE HAVE GENERATED OUR SUCCESSOR, add it to the frontier
                    pQueue.add(northPixel);
                }
            }

            // SOUTH
            Node southPixel = new Node();
            int southX = xVal;
            int southY;
            if (yVal < 499) {
                southY = yVal + 1;
            } // out of bounds check for south edge of terrain
            else {
                southY = yVal;
            }
            int southRGBInt = rgbIntegerPixels[southX][southY];
            if (!(terrainMultiplier.get(southRGBInt) == 0.0)) {
                String southKeyStr = southX + "," + southY;
                if (visitedArr[southX][southY] == 0) {
                    southPixel.setColorTerrainMult(southRGBInt);
                    southPixel.setParent(parentKeyStr);
                    southPixel.setElevation(coordinateElevations[southX][southY]);
                    southPixel.setxCoord(southX);
                    southPixel.setyCoord(southY);
                    coordtoParentPairDict.put(southKeyStr, poppedNode);
                    Double southgOFn = calculategOFn(poppedNode, southPixel);
                    southPixel.setgOFn(southgOFn);
                    Double southHeuristicVal = calculateHeuristic(southPixel,
                            (double) targetX, (double) targetY, targetElevation);
                    southPixel.setHeuristicVal(southHeuristicVal);
                    Double southfOFnEstimatedCost = calculatefOFn(southPixel);
                    southPixel.setfOFn(southfOFnEstimatedCost);
                    Double totalDistanceToSouth = poppedNode.getTotalDistanceToNode() +
                            calculateDistance(poppedNode, southPixel);
                    southPixel.setTotalDistanceToNode(totalDistanceToSouth);
                    pQueue.add(southPixel);
                }
            }

            // EAST
            Node eastPixel = new Node();
            int eastX;
            int eastY = yVal;
            if (xVal < 394) {
                eastX = xVal + 1;
            } // out of bounds check for east edge of terrain
            else {
                eastX = xVal;
            }
            int eastRGBInt = rgbIntegerPixels[eastX][eastY];
            if (!(terrainMultiplier.get(eastRGBInt) == 0.0)) {
                if (visitedArr[eastX][eastY] == 0) {
                    eastPixel.setColorTerrainMult(eastRGBInt);
                    eastPixel.setParent(parentKeyStr);
                    eastPixel.setElevation(coordinateElevations[eastX][eastY]);
                    eastPixel.setxCoord(eastX);
                    eastPixel.setyCoord(eastY);
                    String eastKeyStr = eastX + "," + eastY;
                    coordtoParentPairDict.put(eastKeyStr, poppedNode);
                    Double eastgOFn = calculategOFn(poppedNode, eastPixel);
                    eastPixel.setgOFn(eastgOFn);
                    Double eastHeuristicVal = calculateHeuristic(eastPixel,
                            (double) targetX, (double) targetY, targetElevation);
                    eastPixel.setHeuristicVal(eastHeuristicVal);
                    Double eastfOFnEstimatedCost = calculatefOFn(eastPixel);
                    eastPixel.setfOFn(eastfOFnEstimatedCost);
                    Double totalDistanceToEast = poppedNode.getTotalDistanceToNode() +
                            calculateDistance(poppedNode, eastPixel);
                    eastPixel.setTotalDistanceToNode(totalDistanceToEast);
                    pQueue.add(eastPixel);
                }
            }

            // WEST
            Node westPixel = new Node();
            int westX;
            int westY = yVal;
            if (xVal > 0) {
                westX = xVal - 1;
            } // out of bounds check for west edge of terrain
            else {
                westX = xVal;
            }
            int westRGBInt = rgbIntegerPixels[westX][westY];
            if (!(terrainMultiplier.get(westRGBInt) == 0.0)) {
                if (visitedArr[westX][westY] == 0) {
                    westPixel.setColorTerrainMult(westRGBInt);
                    westPixel.setParent(parentKeyStr);
                    westPixel.setElevation(coordinateElevations[westX][westY]);
                    westPixel.setxCoord(westX);
                    westPixel.setyCoord(westY);
                    String westKeyStr = westX + "," + westY;
                    coordtoParentPairDict.put(westKeyStr, poppedNode);
                    Double westgOFn = calculategOFn(poppedNode, westPixel);
                    westPixel.setgOFn(westgOFn);
                    Double westHeuristicVal = calculateHeuristic(westPixel,
                            (double) targetX, (double) targetY, targetElevation);
                    westPixel.setHeuristicVal(westHeuristicVal);
                    Double westfOFnEstimatedCost = calculatefOFn(westPixel);
                    westPixel.setfOFn(westfOFnEstimatedCost);
                    Double totalDistanceToWest = poppedNode.getTotalDistanceToNode() +
                            calculateDistance(poppedNode, westPixel);
                    westPixel.setTotalDistanceToNode(totalDistanceToWest);
                    pQueue.add(westPixel);
                }
            }

            // Only set as parent once we have generated all our successor children nodes
            visitedArr[xVal][yVal] = 1; // throw our node in the visited set

            // WE HAVE NOW GENERATED THE FOUR SUCCESSOR NODES, and ADDED THEM ALL TO
            // FRONTIER
        }
        // SHOULD NEVER REACH HERE
        System.out.println("something is wrong"); // todo
        return rgbIntegerPixels;
    }

    /**
     *
     * @param plainRGBIntPixels [395][500] integer representation of RGB value for
     *                          all pixels
     * @param pointsToVisit     in order sequence of coordinate points to visit
     * @param elevations        [395][500] pixel/node/coordinate elevations given
     * @return updated pixels (Complete Orienteering Path drawn)
     */
    public static int[][] performOrienteering(int[][] plainRGBIntPixels, ArrayList<int[][]> pointsToVisit,
            double[][] elevations) {
        int[][] cumulativePath = new int[395][500];
        for (int yInt = 0; yInt < 500; yInt++) {
            for (int xInt = 0; xInt < 395; xInt++) {
                // plainPixels[xInt][yInt] = plainRGBIntPixels[xInt][yInt];
                cumulativePath[xInt][yInt] = plainRGBIntPixels[xInt][yInt];
            }
        }
        // a 'control' in Orienteering is a location to visit
        // if we have simply a starting and end point, we have one location to visit
        int numControls = pointsToVisit.size() - 1;
        for (int control = 0; control < numControls; control++) {
            int[][] start = pointsToVisit.get(control);
            int[][] target = pointsToVisit.get(control + 1);
            // create visited Matrix (could turn this into node objects instead)
            // for now this matrix will hold 1s and 0s indicating visited or not visited
            int[][] visitedMatrix = new int[395][500];
            for (int yV = 0; yV < 500; yV++) {
                for (int xV = 0; xV < 395; xV++) {
                    visitedMatrix[xV][yV] = 0;
                }
            }
            // continually pass in our returned cumulativePath, building on top of itself
            cumulativePath = aStarSearch(start, target, plainRGBIntPixels, elevations,
                    visitedMatrix, cumulativePath);
            coordtoParentPairDict.clear();
        }

        return cumulativePath;
    }

    /**
     * Main purpose of main is to read in args appropriately
     * 
     * @param args terrain-image, elevation-file, path-file,
     *             output-img-filename in that order
     */
    public static void main(String[] args) throws IOException {

        // image of dim 395x500 colored pixels, TOP LEFT pixel IS ORIGIN
        //
        // In Java, you can use the ImageIO class to read in an image
        // into a BufferedImage object and get/set pixels from there
        String terrainImg = args[0];
        BufferedImage terrain = null;
        int[][] pixels = new int[395][500];
        try {
            terrain = ImageIO.read(new File(terrainImg));
            for (int y = 0; y < 500; y++) {
                for (int x = 0; x < 395; x++) {
                    // getRGB() returns 8-bit precision int value
                    int pRGB = terrain.getRGB(x, y);
                    pixels[x][y] = pRGB;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // file contains exactly 500 lines of 400 doubles
        // (ignore last 5 doubles on each line)
        String elevationFile = args[1];
        File elevationF = new File(elevationFile);
        Scanner elevationScnnr = new Scanner(elevationF);
        double[][] coordElevations = new double[395][500];
        for (int line = 0; line < 500; line++) {
            for (int item = 0; item < 395; item++) {
                double tempCoordVal = elevationScnnr.nextDouble();
                coordElevations[item][line] = tempCoordVal;
            }
            elevationScnnr.nextLine(); // ignore last 5 vals, skip to next line
        }
        elevationScnnr.close();

        // (x,y) pixel coordinates of Point destinations
        // represented in path-file with (0,0) origin at
        // top left pixel of terrain-image
        String pathFile = args[2]; // points must be visited in order
        File pathPointFile = new File(pathFile);
        Scanner scanner = new Scanner(pathPointFile);
        // every element in pathCoords is an 2D int array which will be 1 row, x&y
        ArrayList<int[][]> pathCoords = new ArrayList<>(); // ArrayList accomodates
        while (scanner.hasNextLine()) { // unknown # of pathCoords
            int[][] temp = new int[1][2];
            try {
                temp[0][0] = scanner.nextInt();
                temp[0][1] = scanner.nextInt();
                pathCoords.add(temp);
            } catch (NoSuchElementException e) { // extra lines at end of file
                break;
            }
        }
        scanner.close();

        // create global terrain multiplier dictionary
        setTerrainMultiplier(terrainMultiplier);
        int[][] updatedPixels = performOrienteering(pixels, pathCoords, coordElevations);
        // we will now use the updated pixels to create out output image
        // with the traversed path drawn over it
        System.out.println("Total path length (m) traversed: " + totalOrienteeringPathDistance);

        // name of output image file to be drawn over by path
        String outputImg = args[3];
        String resourcePath = "";
        String outputfile = resourcePath + outputImg;

        for (int y = 0; y < 500; y++) {
            for (int x = 0; x < 395; x++) {
                int outputPixel = updatedPixels[x][y];
                // setRGB() returns .....
                terrain.setRGB(x, y, outputPixel);
            }
        }
        try {
            ImageIO.write(terrain, "png", new File(outputfile));
        } catch (IOException e) {
            System.out.println(e);
        }

    }

}

/*
 *  Licensed to GIScience Research Group, Heidelberg University (GIScience)
 *
 *   http://www.giscience.uni-hd.de
 *   http://www.heigit.org
 *
 *  under one or more contributor license agreements. See the NOTICE file 
 *  distributed with this work for additional information regarding copyright 
 *  ownership. The GIScience licenses this file to you under the Apache License, 
 *  Version 2.0 (the "License"); you may not use this file except in compliance 
 *  with the License. You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package heigit.ors.routing.algorithms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.cursors.IntObjectCursor;
import com.graphhopper.coll.GHIntObjectHashMap;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.ByteArrayBuffer;
import com.graphhopper.util.CHEdgeIteratorState;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;

public class SubGraph {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private GHIntObjectHashMap<EdgeIteratorLink> _node2edgesMap;
	private Graph _baseGraph;

	class EdgeIteratorLink  {
		public EdgeIteratorState state;
		public EdgeIteratorLink next;


		public EdgeIteratorLink(EdgeIteratorState iterState)
		{
			state = iterState;
		}
	};

	class SubGraphEdgeExplorer implements EdgeExplorer  {
		private SubGraph _graph;

		public SubGraphEdgeExplorer(SubGraph graph)
		{
			_graph = graph;
		}

		@Override
		public EdgeIterator setBaseNode(int baseNode) {
			return _graph.setBaseNode(baseNode);
		}
	};

	class EdgeIteratorLinkIterator implements EdgeIterator, CHEdgeIteratorState
	{
		private EdgeIteratorState _currState;
		private EdgeIteratorLink _link;
		private boolean _firstRun = true;

		public EdgeIteratorLinkIterator(EdgeIteratorLink link)
		{
			_link = link;
			_currState = link.state;
		}

		@Override
		public int getEdge() {
			return _currState.getEdge();
		}

		@Override
		public int getOriginalEdge() {
			return _currState.getOriginalEdge();
		}

		@Override
		public int getBaseNode() {
			return _currState.getBaseNode();
		}

		@Override
		public int getAdjNode() {
			return _currState.getAdjNode();
		}

		@Override
		public PointList fetchWayGeometry(int mode) {
			return null;
		}

		@Override
		public PointList fetchWayGeometry(int mode, ByteArrayBuffer buffer) {
			return null;
		}

		@Override
		public EdgeIteratorState setWayGeometry(PointList list) {
			return null;
		}

		@Override
		public double getDistance() {
			return _currState.getDistance();
		}

		@Override
		public EdgeIteratorState setDistance(double dist) {
			return null;
		}

		@Override
		public long getFlags() {
			return _currState.getFlags();
		}

		@Override
		public long getFlags(int encoderIndex) {
			return _currState.getFlags(encoderIndex);
		}

		@Override
		public EdgeIteratorState setFlags(long flags) {
			return _currState.setFlags(flags);
		}

		@Override
		public int getAdditionalField() {
			return 0;
		}

		@Override
		public EdgeIteratorState setAdditionalField(int value) {
			return null;
		}

		@Override
		public boolean isForward(FlagEncoder encoder) {
			return _currState.isForward(encoder);
		}

		@Override
		public boolean isBackward(FlagEncoder encoder) {
			return _currState.isBackward(encoder);
		}

		@Override
		public boolean getBool(int key, boolean _default) {
			return _currState.getBool(key, _default);
		}

		@Override
		public String getName() {
			return _currState.getName();
		}

		@Override
		public EdgeIteratorState setName(String name) {
			return null;
		}

		@Override
		public EdgeIteratorState detach(boolean reverse) {
			return _currState.detach(reverse);
		}

		@Override
		public EdgeIteratorState copyPropertiesTo(EdgeIteratorState e) {
			return null;
		}

		@Override
		public boolean next() {
			if (_firstRun)
			{
				_firstRun = false;
				return true;
			}

			_link = _link.next;

			if (_link == null)
			{
				_currState = null;

				return false;
			}

			_currState = _link.state;

			return true;
		}

		@Override
		public int getSkippedEdge1() {
			return 0;
		}

		@Override
		public int getSkippedEdge2() {
			return 0;
		}

		@Override
		public void setSkippedEdges(int edge1, int edge2) {
		}

		@Override
		public boolean isShortcut() {
			if (_currState instanceof CHEdgeIteratorState)
				return (((CHEdgeIteratorState) _currState).isShortcut());
			else 
				return false;
		}

		@Override
		public int getMergeStatus(long flags) {
			return 0;
		}

		@Override
		public double getWeight() {
			return (((CHEdgeIteratorState) _currState).getWeight());
		}

		@Override
		public CHEdgeIteratorState setWeight(double weight) {
			return null;
		}
	};

	public SubGraph(Graph graph)
	{
		_baseGraph = graph;
		_node2edgesMap = new GHIntObjectHashMap<EdgeIteratorLink>(Math.min(Math.max(200, graph.getNodes() / 10), 2000));
	}

	/**
	 * Returns true/false depending on whether node is already in the graph or not.
	 */
	public boolean addEdge(int adjNode, EdgeIteratorState iter, boolean reverse)
	{
		if (iter == null)
		{
			_node2edgesMap.put(adjNode, null);
			return true;
		}

		EdgeIteratorState iterState = null;
		if (reverse)
		{
			iterState =  _baseGraph.getEdgeIteratorState(iter.getEdge(), adjNode);
			adjNode = iter.getAdjNode();
		}
		else
		{
			iterState =  _baseGraph.getEdgeIteratorState(iter.getEdge(), iter.getAdjNode());
			adjNode = iter.getBaseNode();
		}

		EdgeIteratorLink link = _node2edgesMap.get(adjNode);
		if (link == null)
		{
			link = new EdgeIteratorLink(iterState);

			_node2edgesMap.put(adjNode, link);
			return true;
		}
		else
		{ 
			while (link.next != null)
				link = link.next;

			link.next = new EdgeIteratorLink(iterState);

			return false;
		}
	}

	public boolean containsNode(int adjNode)
	{
		return _node2edgesMap.containsKey(adjNode);
	}

	public EdgeIterator setBaseNode(int baseNode)
	{
		EdgeIteratorLink link = _node2edgesMap.get(baseNode);
		return link == null ? null: new EdgeIteratorLinkIterator(link);
	}

	public EdgeExplorer createExplorer()
	{
		return new SubGraphEdgeExplorer(this);
	}

	public void print()
	{
		int edgesCount = 0;

		EdgeExplorer explorer = createExplorer();

		for (IntObjectCursor<?> node : _node2edgesMap) {
			EdgeIterator iter = explorer.setBaseNode(node.key);

			if (iter != null)
			{
				while (iter.next())
				{
					edgesCount++;
				}
			}
		}

		logger.info("SubGraph: nodes - " + _node2edgesMap.size() + "; edges - " + edgesCount);
	}
}

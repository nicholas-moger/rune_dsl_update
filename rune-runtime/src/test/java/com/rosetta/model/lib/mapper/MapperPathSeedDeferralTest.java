/*
 * Copyright 2024 REGnosys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rosetta.model.lib.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.expression.ExpressionOperators;
import com.rosetta.model.lib.qualify.QualifyResult;

/**
 * Locks for the U022 deferred path-construction contract (census § 18c): the
 * item stores a package-private path seed in place of the built node and
 * {@link AbstractMapperItem#getPath()} builds the chain memoized on first
 * observation. The pins: the never-observed item builds NO node (the lazy-state
 * pin via the package seam), observed rendering ≡ an eager
 * {@link MapperPath.MapperPathBuilder} twin per channel, the two sharing SHAPES
 * (the error item sharing the SOURCE item's node instance, and the empty-list
 * child hop extending the parent's chain while the public parent channel stays
 * empty), the upcast share, memoize-once identity, the compareTo forcing pin,
 * the benign-race convergence smoke, the QUALIFICATION-channel pin
 * ({@code QualifyResult}'s unconditional {@code getError()} read over a
 * path-bearing success-classed lazy result renders byte-identically), and the
 * pre-built legacy-channel pin (a handed-in {@code MapperPath} is served
 * unchanged). The corpus-scale lock is the rune-equivalence O2 channel + the
 * § 14f text-capture instrument; these pins keep the seed contract enforced
 * without the corpus.
 */
class MapperPathSeedDeferralTest {

	// --- reflection probes (the package seam: the memoized node field) ---

	private static MapperPath builtNode(AbstractMapperItem<?> item) throws Exception {
		Field field = AbstractMapperItem.class.getDeclaredField("path");
		field.setAccessible(true);
		return (MapperPath) field.get(item);
	}

	private static MapperPath parentNode(MapperPath path) throws Exception {
		Field field = MapperPath.class.getDeclaredField("parent");
		field.setAccessible(true);
		return (MapperPath) field.get(path);
	}

	@SuppressWarnings("unchecked")
	private static List<MapperListItem<List<String>, ?>> listOfListsItems(MapperListOfLists<String> mapper) throws Exception {
		Field field = MapperListOfLists.class.getDeclaredField("items");
		field.setAccessible(true);
		return (List<MapperListItem<List<String>, ?>>) field.get(mapper);
	}

	private static <T> MapperItem<? extends T, ?> soleItem(MapperBuilder<T> mapper) {
		List<MapperItem<? extends T, ?>> items = mapper.getItems().collect(Collectors.toList());
		assertEquals(1, items.size(), "expected a single item");
		return items.get(0);
	}

	// --- the pins ---

	@Test
	void neverObservedItemsBuildNoNode() throws Exception {
		Trade trade = new Trade("2026-08-11", Arrays.asList("leg0", "leg1"));

		// root seats
		assertNull(builtNode(soleItem(MapperS.of(trade))), "of(t) root must not build at construction");
		assertNull(builtNode(soleItem(MapperS.ofNull())), "ofNull root must not build at construction");
		assertNull(builtNode(soleItem(MapperS.identity())), "identity root must not build at construction");
		for (MapperItem<? extends String, ?> item : MapperC.of(Arrays.asList("a", null, "b"))
				.getItems().collect(Collectors.toList())) {
			assertNull(builtNode(item), "list-lane roots must not build at construction");
		}
		for (MapperListItem<List<String>, ?> item : listOfListsItems(
				MapperListOfLists.of(Arrays.asList(Arrays.asList("x"), Arrays.asList("y"))))) {
			assertNull(builtNode(item), "list-of-lists roots must not build at construction");
		}

		// hop seats — and the hop must not force the parent either
		MapperS<Trade> root = MapperS.of(trade);
		MapperS<String> hop = root.map("getTradeDate", Trade::getTradeDate);
		assertNull(builtNode(soleItem(hop)), "the single hop must not build at construction");
		assertNull(builtNode(soleItem(root)), "constructing a hop must not force the parent's node");
		for (MapperItem<? extends String, ?> item : root.mapC("getLegs", Trade::getLegs)
				.getItems().collect(Collectors.toList())) {
			assertNull(builtNode(item), "list hops must not build at construction");
		}

		// the error share and the upcast twin
		MapperS<String> errorShare = MapperS.<Trade>ofNull().map("getTradeDate", Trade::getTradeDate);
		assertNull(builtNode(soleItem(errorShare)), "the error share must not build at construction");
		MapperItem<? extends String, ?> source = soleItem(hop);
		MapperItem<Object, ?> twin = source.upcast();
		assertNull(builtNode(twin), "upcast must not build at construction");
		assertNull(builtNode(source), "upcast must not force the source's node");
	}

	@Test
	void observedRenderingMatchesTheEagerTwinPerChannel() throws Exception {
		Trade trade = new Trade("2026-08-11", Arrays.asList("leg0", "leg1"));

		MapperS<String> single = MapperS.of(trade).map("getTradeDate", Trade::getTradeDate);
		MapperPath eagerHop = MapperPath.builder().addRoot(Trade.class).toBuilder().addFunctionName("getTradeDate");
		MapperPath observed = (MapperPath) single.getPaths().get(0);
		assertEquals(eagerHop, observed, "the deferred chain must value-equal the eager twin");
		assertEquals(eagerHop.getFullPath(), observed.getFullPath());
		assertEquals(eagerHop.getNames(), observed.getNames());
		assertEquals(eagerHop.getGetters(), observed.getGetters());
		assertEquals(eagerHop.getLastName(), observed.getLastName());
		assertEquals(eagerHop.toString(), observed.toString());
		assertEquals(eagerHop.hashCode(), observed.hashCode());
		assertEquals(0, eagerHop.compareTo(observed));
		assertEquals(eagerHop.toString(), single.toString(), "the MapperS toString channel");

		MapperC<String> legs = MapperS.of(trade).mapC("getLegs", Trade::getLegs);
		assertEquals(Arrays.asList("Trade->getLegs[0]", "Trade->getLegs[1]"),
				legs.getPaths().stream().map(Mapper.Path::toString).collect(Collectors.toList()),
				"the list-hop getPaths channel");
		assertEquals("Trade->getLegs[0],Trade->getLegs[1]", legs.toString(), "the MapperC toString channel");

		MapperS<String> nullRoot = MapperS.ofNull();
		assertEquals("[Null]", nullRoot.getErrorPaths().toString(), "the getErrorPaths channel");
		assertEquals(Collections.singletonList("Null was null"), nullRoot.getErrors(), "the getErrors channel");
		assertEquals("Null", nullRoot.toString());

		List<String> innerList = Arrays.asList("x");
		MapperListItem<List<String>, ?> listRoot = listOfListsItems(
				MapperListOfLists.of(Arrays.asList(innerList))).get(0);
		assertEquals(MapperPath.builder().addRoot(innerList.getClass()).getFullPath(),
				listRoot.getPath().getFullPath(), "the list-of-lists root must render its eager twin");
	}

	@Test
	void errorItemSharesTheSourceNodeInstance() throws Exception {
		Trade dateless = new Trade(null, Arrays.asList("leg0"));
		MapperS<Trade> root = MapperS.of(dateless);
		MapperS<String> failedHop = root.map("getTradeDate", Trade::getTradeDate);
		MapperItem<? extends String, ?> sourceItem = soleItem(failedHop);
		assertTrue(sourceItem.isError(), "the null-valued hop must be the error source");

		// the single-hop share (the getMapperItem error branch)
		MapperS<Integer> shared = failedHop.map("getLength", String::length);
		MapperItem<? extends Integer, ?> sharedItem = soleItem(shared);
		assertTrue(sharedItem.isError());
		assertEquals(Optional.empty(), sharedItem.getParentItem(), "the public parent channel must stay empty");
		assertNull(builtNode(sharedItem), "the share must stay unbuilt until observed");
		MapperPath sharedNode = sharedItem.getPath();
		assertSame(sourceItem.getPath(), sharedNode, "the error item's node must BE the source's node instance");
		assertEquals("Trade->getTradeDate", sharedNode.getFullPath(), "the shared node must render the source path");

		// the list-hop share (the getMapperItems error branch)
		MapperC<String> sharedList = failedHop.mapC("getList", s -> Arrays.asList(s));
		MapperItem<? extends String, ?> sharedListItem = soleItem(sharedList);
		assertTrue(sharedListItem.isError());
		assertEquals(Optional.empty(), sharedListItem.getParentItem());
		assertSame(sourceItem.getPath(), sharedListItem.getPath(),
				"the list-route error item must share the same node instance");
	}

	@Test
	void emptyListChildHopExtendsTheParentChainWithTheEmptyParentChannel() throws Exception {
		Trade leglessTrade = new Trade("2026-08-11", Collections.emptyList());
		MapperS<Trade> root = MapperS.of(leglessTrade);
		MapperItem<? extends Trade, ?> rootItem = soleItem(root);
		MapperC<String> legs = root.mapC("getLegs", Trade::getLegs);
		MapperItem<? extends String, ?> errorItem = soleItem(legs);

		assertTrue(errorItem.isError());
		assertEquals(Optional.empty(), errorItem.getParentItem(), "the public parent channel must stay empty");
		assertNull(builtNode(errorItem), "the child hop must stay unbuilt until observed");

		MapperPath childPath = errorItem.getPath();
		assertEquals("Trade->getLegs", childPath.getFullPath(), "the child hop must render the extended path");
		assertSame(rootItem.getPath(), parentNode(childPath),
				"the child hop's prefix must BE the parent item's memoized node instance");
	}

	@Test
	void upcastSharesTheSourceNodeInstance() throws Exception {
		Trade trade = new Trade("2026-08-11", Arrays.asList("leg0"));
		MapperS<String> hop = MapperS.of(trade).map("getTradeDate", Trade::getTradeDate);
		MapperItem<? extends String, ?> source = soleItem(hop);
		MapperItem<Object, ?> twin = source.upcast();

		assertSame(source.getParentItem().get(), twin.getParentItem().get(),
				"the public parent channel must carry over the same parent item");
		MapperPath twinNode = twin.getPath();
		assertSame(source.getPath(), twinNode, "the re-typed twin must share the source's node instance");
		assertEquals("Trade->getTradeDate", twinNode.getFullPath());
	}

	@Test
	void observedNodesMemoizeOncePerItem() throws Exception {
		Trade trade = new Trade("2026-08-11", Collections.emptyList());
		MapperS<Trade> root = MapperS.of(trade);
		MapperItem<? extends Trade, ?> rootItem = soleItem(root);
		assertSame(rootItem.getPath(), rootItem.getPath(), "the root node must memoize once");

		MapperS<String> hop = root.map("getTradeDate", Trade::getTradeDate);
		MapperItem<? extends String, ?> hopItem = soleItem(hop);
		assertSame(hopItem.getPath(), hopItem.getPath(), "the hop node must memoize once");

		MapperItem<? extends String, ?> childHopItem = soleItem(root.mapC("getLegs", Trade::getLegs));
		assertSame(childHopItem.getPath(), childHopItem.getPath(), "the child-hop node must memoize once");

		MapperItem<? extends Integer, ?> shareItem = soleItem(
				MapperS.<Trade>ofNull().map("getTradeDate", Trade::getTradeDate).map("getLength", String::length));
		assertSame(shareItem.getPath(), shareItem.getPath(), "the shared node must memoize once");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	void compareToForcesTheBuildAndMatchesTheEagerOrdering() throws Exception {
		Trade trade = new Trade("2026-08-11", Arrays.asList("leg0"));
		MapperItem<? extends String, ?> alpha = soleItem(MapperS.of(trade).map("getAlpha", t -> "a"));
		MapperItem<? extends String, ?> beta = soleItem(MapperS.of(trade).map("getBeta", t -> "b"));
		assertNull(builtNode(alpha));
		assertNull(builtNode(beta));

		int itemOrder = ((AbstractMapperItem) alpha).compareTo(beta);
		MapperPath eagerAlpha = MapperPath.builder().addRoot(Trade.class).toBuilder().addFunctionName("getAlpha");
		MapperPath eagerBeta = MapperPath.builder().addRoot(Trade.class).toBuilder().addFunctionName("getBeta");
		assertEquals(eagerAlpha.compareTo(eagerBeta), itemOrder, "item ordering must match the eager path ordering");
		assertTrue(itemOrder < 0);
		assertNotNull(builtNode(alpha), "compareTo must force the left build");
		assertNotNull(builtNode(beta), "compareTo must force the right build");
	}

	@Test
	void concurrentObserversConvergeOnTheEagerRendering() throws Exception {
		Trade trade = new Trade("2026-08-11", Arrays.asList("leg0"));
		MapperS<String> deep = MapperS.of(trade)
				.map("getTradeLot", t -> t)
				.map("getPriceQuantity", t -> t)
				.map("getLeg2Payer", Trade::getTradeDate);
		MapperItem<? extends String, ?> item = soleItem(deep);
		String expectedFullPath = "Trade->getTradeLot->getPriceQuantity->getLeg2Payer";

		int threads = 8;
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);
		List<Throwable> failures = new CopyOnWriteArrayList<>();
		for (int i = 0; i < threads; i++) {
			Thread reader = new Thread(() -> {
				try {
					start.await();
					assertEquals(expectedFullPath, item.getPath().getFullPath());
				} catch (Throwable t) {
					failures.add(t);
				} finally {
					done.countDown();
				}
			});
			// Daemon so a pathologically-blocked reader can never keep the
			// test JVM alive past the await timeout below.
			reader.setDaemon(true);
			reader.start();
		}
		start.countDown();
		assertTrue(done.await(30, java.util.concurrent.TimeUnit.SECONDS), "concurrent observers must finish");
		assertTrue(failures.isEmpty(), () -> "concurrent observers must all see the eager rendering: " + failures);
		assertSame(item.getPath(), item.getPath(), "the memo must have settled on one instance");
	}

	@Test
	void preBuiltPathSeedServesTheHandedInNodeUnchanged() {
		MapperPath prebuilt = MapperPath.builder().addRoot(Trade.class).toBuilder().addFunctionName("getTradeDate");
		MapperS<String> handed = MapperS.of("2026-08-11", prebuilt, null);
		MapperItem<? extends String, ?> item = soleItem(handed);
		assertSame(prebuilt, item.getPath(), "the legacy channel must serve the handed-in node instance");
		assertSame(prebuilt, item.getPath(), "and memoize it");
		assertEquals("Trade->getTradeDate", item.getPath().getFullPath());
	}

	@Test
	void qualificationChannelRendersByteIdenticallyOverDeferredPaths() {
		Trade trade = new Trade("2026-08-11", Arrays.asList("leg0"));
		MapperS<String> pathBearing = MapperS.of(trade).map("getTradeDate", Trade::getTradeDate);
		MapperS<String> emptyOperand = MapperS.ofNull();

		ComparisonResult result = ExpressionOperators.notEqual(pathBearing, emptyOperand, CardinalityOperator.All);
		assertTrue(result.get(), "the empty-operand comparison must stay success-classed");

		QualifyResult qualified = QualifyResult.builder()
				.setName("qualification")
				.setDefinition("definition")
				.setExpressionResult("definition", result)
				.build();
		QualifyResult.ExpressionDataRuleResult expression = qualified.getExpressionDataRuleResults().iterator().next();

		MapperPath eagerHop = MapperPath.builder().addRoot(Trade.class).toBuilder().addFunctionName("getTradeDate");
		String expected = "[" + eagerHop.getFullPath() + "] [2026-08-11] cannot be compared to ["
				+ MapperPath.builder().addNull().getFullPath() + "]";
		assertEquals(expected, expression.getError(),
				"the qualification channel's unconditional getError read must render the eager text byte-identically");
	}

	private static final class Trade {
		private final String tradeDate;
		private final List<String> legs;

		private Trade(String tradeDate, List<String> legs) {
			this.tradeDate = tradeDate;
			this.legs = legs;
		}

		public String getTradeDate() {
			return tradeDate;
		}

		public List<String> getLegs() {
			return legs;
		}
	}
}

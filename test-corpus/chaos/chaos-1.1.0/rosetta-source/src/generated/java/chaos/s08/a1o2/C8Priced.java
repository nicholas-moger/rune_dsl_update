package chaos.s08.a1o2;

import chaos.s08.a1o2.meta.C8PricedMeta;
import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.Required;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.util.ListEquals;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Alias-typed attributes at every cardinality.
 * @version 1.0.0
 */
@RosettaDataType(value="C8Priced", builder=C8Priced.C8PricedBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C8Priced", model="chaos", builder=C8Priced.C8PricedBuilderImpl.class, version="1.0.0")
public interface C8Priced extends RosettaModelObject {

	C8PricedMeta metaData = new C8PricedMeta();

	/*********************** Getter Methods  ***********************/
	Integer getQty();
	String getCcy();
	List<BigDecimal> getWeights();
	List<Integer> getEvens();
	C8Box getBox();

	/*********************** Build Methods  ***********************/
	C8Priced build();
	
	C8Priced.C8PricedBuilder toBuilder();
	
	static C8Priced.C8PricedBuilder builder() {
		return new C8Priced.C8PricedBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C8Priced> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C8Priced> getType() {
		return C8Priced.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("qty"), Integer.class, getQty(), this);
		processor.processBasic(path.newSubPath("ccy"), String.class, getCcy(), this);
		processor.processBasic(path.newSubPath("weights"), BigDecimal.class, getWeights(), this);
		processor.processBasic(path.newSubPath("evens"), Integer.class, getEvens(), this);
		processRosetta(path.newSubPath("box"), processor, C8Box.class, getBox());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C8PricedBuilder extends C8Priced, RosettaModelObjectBuilder {
		C8Box.C8BoxBuilder getOrCreateBox();
		@Override
		C8Box.C8BoxBuilder getBox();
		C8Priced.C8PricedBuilder setQty(Integer qty);
		C8Priced.C8PricedBuilder setCcy(String ccy);
		C8Priced.C8PricedBuilder addWeights(BigDecimal weights);
		C8Priced.C8PricedBuilder addWeights(BigDecimal weights, int idx);
		C8Priced.C8PricedBuilder addWeights(List<BigDecimal> weights);
		C8Priced.C8PricedBuilder setWeights(List<BigDecimal> weights);
		C8Priced.C8PricedBuilder addEvens(Integer evens);
		C8Priced.C8PricedBuilder addEvens(Integer evens, int idx);
		C8Priced.C8PricedBuilder addEvens(List<Integer> evens);
		C8Priced.C8PricedBuilder setEvens(List<Integer> evens);
		C8Priced.C8PricedBuilder setBox(C8Box box);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("qty"), Integer.class, getQty(), this);
			processor.processBasic(path.newSubPath("ccy"), String.class, getCcy(), this);
			processor.processBasic(path.newSubPath("weights"), BigDecimal.class, getWeights(), this);
			processor.processBasic(path.newSubPath("evens"), Integer.class, getEvens(), this);
			processRosetta(path.newSubPath("box"), processor, C8Box.C8BoxBuilder.class, getBox());
		}
		

		C8Priced.C8PricedBuilder prune();
	}

	/*********************** Immutable Implementation of C8Priced  ***********************/
	class C8PricedImpl implements C8Priced {
		private final Integer qty;
		private final String ccy;
		private final List<BigDecimal> weights;
		private final List<Integer> evens;
		private final C8Box box;
		
		protected C8PricedImpl(C8Priced.C8PricedBuilder builder) {
			this.qty = builder.getQty();
			this.ccy = builder.getCcy();
			this.weights = ofNullable(builder.getWeights()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.evens = ofNullable(builder.getEvens()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.box = ofNullable(builder.getBox()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("qty")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("qty")
		public Integer getQty() {
			return qty;
		}
		
		@Override
		@RosettaAttribute("ccy")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("ccy")
		public String getCcy() {
			return ccy;
		}
		
		@Override
		@RosettaAttribute("weights")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("weights")
		public List<BigDecimal> getWeights() {
			return weights;
		}
		
		@Override
		@RosettaAttribute("evens")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("evens")
		public List<Integer> getEvens() {
			return evens;
		}
		
		@Override
		@RosettaAttribute("box")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("box")
		public C8Box getBox() {
			return box;
		}
		
		@Override
		public C8Priced build() {
			return this;
		}
		
		@Override
		public C8Priced.C8PricedBuilder toBuilder() {
			C8Priced.C8PricedBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C8Priced.C8PricedBuilder builder) {
			ofNullable(getQty()).ifPresent(builder::setQty);
			ofNullable(getCcy()).ifPresent(builder::setCcy);
			ofNullable(getWeights()).ifPresent(builder::setWeights);
			ofNullable(getEvens()).ifPresent(builder::setEvens);
			ofNullable(getBox()).ifPresent(builder::setBox);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C8Priced _that = getType().cast(o);
		
			if (!Objects.equals(qty, _that.getQty())) return false;
			if (!Objects.equals(ccy, _that.getCcy())) return false;
			if (!ListEquals.listEquals(weights, _that.getWeights())) return false;
			if (!ListEquals.listEquals(evens, _that.getEvens())) return false;
			if (!Objects.equals(box, _that.getBox())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (qty != null ? qty.hashCode() : 0);
			_result = 31 * _result + (ccy != null ? ccy.hashCode() : 0);
			_result = 31 * _result + (weights != null ? weights.hashCode() : 0);
			_result = 31 * _result + (evens != null ? evens.hashCode() : 0);
			_result = 31 * _result + (box != null ? box.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C8Priced {" +
				"qty=" + this.qty + ", " +
				"ccy=" + this.ccy + ", " +
				"weights=" + this.weights + ", " +
				"evens=" + this.evens + ", " +
				"box=" + this.box +
			'}';
		}
	}

	/*********************** Builder Implementation of C8Priced  ***********************/
	class C8PricedBuilderImpl implements C8Priced.C8PricedBuilder {
	
		protected Integer qty;
		protected String ccy;
		protected List<BigDecimal> weights = new ArrayList<>();
		protected List<Integer> evens = new ArrayList<>();
		protected C8Box.C8BoxBuilder box;
		
		@Override
		@RosettaAttribute("qty")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("qty")
		public Integer getQty() {
			return qty;
		}
		
		@Override
		@RosettaAttribute("ccy")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("ccy")
		public String getCcy() {
			return ccy;
		}
		
		@Override
		@RosettaAttribute("weights")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("weights")
		public List<BigDecimal> getWeights() {
			return weights;
		}
		
		@Override
		@RosettaAttribute("evens")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("evens")
		public List<Integer> getEvens() {
			return evens;
		}
		
		@Override
		@RosettaAttribute("box")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("box")
		public C8Box.C8BoxBuilder getBox() {
			return box;
		}
		
		@Override
		public C8Box.C8BoxBuilder getOrCreateBox() {
			C8Box.C8BoxBuilder result;
			if (box!=null) {
				result = box;
			}
			else {
				result = box = C8Box.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("qty")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("qty")
		@Override
		public C8Priced.C8PricedBuilder setQty(Integer _qty) {
			this.qty = _qty == null ? null : _qty;
			return this;
		}
		
		@RosettaAttribute("ccy")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("ccy")
		@Override
		public C8Priced.C8PricedBuilder setCcy(String _ccy) {
			this.ccy = _ccy == null ? null : _ccy;
			return this;
		}
		
		@RosettaAttribute("weights")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("weights")
		@Override
		public C8Priced.C8PricedBuilder addWeights(BigDecimal _weights) {
			if (_weights != null) {
				this.weights.add(_weights);
			}
			return this;
		}
		
		@Override
		public C8Priced.C8PricedBuilder addWeights(BigDecimal _weights, int idx) {
			getIndex(this.weights, idx, () -> _weights);
			return this;
		}
		
		@Override
		public C8Priced.C8PricedBuilder addWeights(List<BigDecimal> weightss) {
			if (weightss != null) {
				for (final BigDecimal toAdd : weightss) {
					this.weights.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("weights")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("weights")
		@Override
		public C8Priced.C8PricedBuilder setWeights(List<BigDecimal> weightss) {
			if (weightss == null) {
				this.weights = new ArrayList<>();
			} else {
				this.weights = weightss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("evens")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("evens")
		@Override
		public C8Priced.C8PricedBuilder addEvens(Integer _evens) {
			if (_evens != null) {
				this.evens.add(_evens);
			}
			return this;
		}
		
		@Override
		public C8Priced.C8PricedBuilder addEvens(Integer _evens, int idx) {
			getIndex(this.evens, idx, () -> _evens);
			return this;
		}
		
		@Override
		public C8Priced.C8PricedBuilder addEvens(List<Integer> evenss) {
			if (evenss != null) {
				for (final Integer toAdd : evenss) {
					this.evens.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("evens")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("evens")
		@Override
		public C8Priced.C8PricedBuilder setEvens(List<Integer> evenss) {
			if (evenss == null) {
				this.evens = new ArrayList<>();
			} else {
				this.evens = evenss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("box")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("box")
		@Override
		public C8Priced.C8PricedBuilder setBox(C8Box _box) {
			this.box = _box == null ? null : _box.toBuilder();
			return this;
		}
		
		@Override
		public C8Priced build() {
			return new C8Priced.C8PricedImpl(this);
		}
		
		@Override
		public C8Priced.C8PricedBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C8Priced.C8PricedBuilder prune() {
			if (box!=null && !box.prune().hasData()) box = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getQty()!=null) return true;
			if (getCcy()!=null) return true;
			if (getWeights()!=null && !getWeights().isEmpty()) return true;
			if (getEvens()!=null && !getEvens().isEmpty()) return true;
			if (getBox()!=null && getBox().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C8Priced.C8PricedBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C8Priced.C8PricedBuilder o = (C8Priced.C8PricedBuilder) other;
			
			merger.mergeRosetta(getBox(), o.getBox(), this::setBox);
			
			merger.mergeBasic(getQty(), o.getQty(), this::setQty);
			merger.mergeBasic(getCcy(), o.getCcy(), this::setCcy);
			merger.mergeBasic(getWeights(), o.getWeights(), (Consumer<BigDecimal>) this::addWeights);
			merger.mergeBasic(getEvens(), o.getEvens(), (Consumer<Integer>) this::addEvens);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C8Priced _that = getType().cast(o);
		
			if (!Objects.equals(qty, _that.getQty())) return false;
			if (!Objects.equals(ccy, _that.getCcy())) return false;
			if (!ListEquals.listEquals(weights, _that.getWeights())) return false;
			if (!ListEquals.listEquals(evens, _that.getEvens())) return false;
			if (!Objects.equals(box, _that.getBox())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (qty != null ? qty.hashCode() : 0);
			_result = 31 * _result + (ccy != null ? ccy.hashCode() : 0);
			_result = 31 * _result + (weights != null ? weights.hashCode() : 0);
			_result = 31 * _result + (evens != null ? evens.hashCode() : 0);
			_result = 31 * _result + (box != null ? box.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C8PricedBuilder {" +
				"qty=" + this.qty + ", " +
				"ccy=" + this.ccy + ", " +
				"weights=" + this.weights + ", " +
				"evens=" + this.evens + ", " +
				"box=" + this.box +
			'}';
		}
	}
}

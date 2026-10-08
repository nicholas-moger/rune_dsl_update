package test.chswitchedge;

import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import test.chswitchedge.meta.BagMeta;

import static java.util.Optional.ofNullable;

/**
 * The rule and datarule carrier.
 * @version 0.0.0
 */
@RosettaDataType(value="Bag", builder=Bag.BagBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Bag", model="test", builder=Bag.BagBuilderImpl.class, version="0.0.0")
public interface Bag extends RosettaModelObject {

	BagMeta metaData = new BagMeta();

	/*********************** Getter Methods  ***********************/
	List<? extends Either> getEths();

	/*********************** Build Methods  ***********************/
	Bag build();
	
	Bag.BagBuilder toBuilder();
	
	static Bag.BagBuilder builder() {
		return new Bag.BagBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Bag> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Bag> getType() {
		return Bag.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("eths"), processor, Either.class, getEths());
	}
	

	/*********************** Builder Interface  ***********************/
	interface BagBuilder extends Bag, RosettaModelObjectBuilder {
		Either.EitherBuilder getOrCreateEths(int index);
		@Override
		List<? extends Either.EitherBuilder> getEths();
		Bag.BagBuilder addEths(Either eths);
		Bag.BagBuilder addEths(Either eths, int idx);
		Bag.BagBuilder addEths(List<? extends Either> eths);
		Bag.BagBuilder setEths(List<? extends Either> eths);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("eths"), processor, Either.EitherBuilder.class, getEths());
		}
		

		Bag.BagBuilder prune();
	}

	/*********************** Immutable Implementation of Bag  ***********************/
	class BagImpl implements Bag {
		private final List<? extends Either> eths;
		
		protected BagImpl(Bag.BagBuilder builder) {
			this.eths = ofNullable(builder.getEths()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
		}
		
		@Override
		@RosettaAttribute("eths")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("eths")
		public List<? extends Either> getEths() {
			return eths;
		}
		
		@Override
		public Bag build() {
			return this;
		}
		
		@Override
		public Bag.BagBuilder toBuilder() {
			Bag.BagBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Bag.BagBuilder builder) {
			ofNullable(getEths()).ifPresent(builder::setEths);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Bag _that = getType().cast(o);
		
			if (!ListEquals.listEquals(eths, _that.getEths())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (eths != null ? eths.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Bag {" +
				"eths=" + this.eths +
			'}';
		}
	}

	/*********************** Builder Implementation of Bag  ***********************/
	class BagBuilderImpl implements Bag.BagBuilder {
	
		protected List<Either.EitherBuilder> eths = new ArrayList<>();
		
		@Override
		@RosettaAttribute("eths")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("eths")
		public List<? extends Either.EitherBuilder> getEths() {
			return eths;
		}
		
		@Override
		public Either.EitherBuilder getOrCreateEths(int index) {
			if (eths==null) {
				this.eths = new ArrayList<>();
			}
			return getIndex(eths, index, () -> {
						Either.EitherBuilder newEths = Either.builder();
						return newEths;
					});
		}
		
		@RosettaAttribute("eths")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("eths")
		@Override
		public Bag.BagBuilder addEths(Either _eths) {
			if (_eths != null) {
				this.eths.add(_eths.toBuilder());
			}
			return this;
		}
		
		@Override
		public Bag.BagBuilder addEths(Either _eths, int idx) {
			getIndex(this.eths, idx, () -> _eths.toBuilder());
			return this;
		}
		
		@Override
		public Bag.BagBuilder addEths(List<? extends Either> ethss) {
			if (ethss != null) {
				for (final Either toAdd : ethss) {
					this.eths.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("eths")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("eths")
		@Override
		public Bag.BagBuilder setEths(List<? extends Either> ethss) {
			if (ethss == null) {
				this.eths = new ArrayList<>();
			} else {
				this.eths = ethss.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public Bag build() {
			return new Bag.BagImpl(this);
		}
		
		@Override
		public Bag.BagBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Bag.BagBuilder prune() {
			eths = eths.stream().filter(b->b!=null).<Either.EitherBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getEths()!=null && getEths().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Bag.BagBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Bag.BagBuilder o = (Bag.BagBuilder) other;
			
			merger.mergeRosetta(getEths(), o.getEths(), this::getOrCreateEths);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Bag _that = getType().cast(o);
		
			if (!ListEquals.listEquals(eths, _that.getEths())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (eths != null ? eths.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "BagBuilder {" +
				"eths=" + this.eths +
			'}';
		}
	}
}

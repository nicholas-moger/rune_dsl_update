package chaos.s29.a2alias;

import chaos.s29.a2alias.meta.C29BagMeta;
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

import static java.util.Optional.ofNullable;

/**
 * The rule and datarule carrier.
 * @version 1.0.0
 */
@RosettaDataType(value="C29Bag", builder=C29Bag.C29BagBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C29Bag", model="chaos", builder=C29Bag.C29BagBuilderImpl.class, version="1.0.0")
public interface C29Bag extends RosettaModelObject {

	C29BagMeta metaData = new C29BagMeta();

	/*********************** Getter Methods  ***********************/
	C29Outer getOuter();
	List<? extends C29Outer> getOuters();

	/*********************** Build Methods  ***********************/
	C29Bag build();
	
	C29Bag.C29BagBuilder toBuilder();
	
	static C29Bag.C29BagBuilder builder() {
		return new C29Bag.C29BagBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C29Bag> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C29Bag> getType() {
		return C29Bag.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("outer"), processor, C29Outer.class, getOuter());
		processRosetta(path.newSubPath("outers"), processor, C29Outer.class, getOuters());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C29BagBuilder extends C29Bag, RosettaModelObjectBuilder {
		C29Outer.C29OuterBuilder getOrCreateOuter();
		@Override
		C29Outer.C29OuterBuilder getOuter();
		C29Outer.C29OuterBuilder getOrCreateOuters(int index);
		@Override
		List<? extends C29Outer.C29OuterBuilder> getOuters();
		C29Bag.C29BagBuilder setOuter(C29Outer outer);
		C29Bag.C29BagBuilder addOuters(C29Outer outers);
		C29Bag.C29BagBuilder addOuters(C29Outer outers, int idx);
		C29Bag.C29BagBuilder addOuters(List<? extends C29Outer> outers);
		C29Bag.C29BagBuilder setOuters(List<? extends C29Outer> outers);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("outer"), processor, C29Outer.C29OuterBuilder.class, getOuter());
			processRosetta(path.newSubPath("outers"), processor, C29Outer.C29OuterBuilder.class, getOuters());
		}
		

		C29Bag.C29BagBuilder prune();
	}

	/*********************** Immutable Implementation of C29Bag  ***********************/
	class C29BagImpl implements C29Bag {
		private final C29Outer outer;
		private final List<? extends C29Outer> outers;
		
		protected C29BagImpl(C29Bag.C29BagBuilder builder) {
			this.outer = ofNullable(builder.getOuter()).map(f->f.build()).orElse(null);
			this.outers = ofNullable(builder.getOuters()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
		}
		
		@Override
		@RosettaAttribute("outer")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("outer")
		public C29Outer getOuter() {
			return outer;
		}
		
		@Override
		@RosettaAttribute("outers")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("outers")
		public List<? extends C29Outer> getOuters() {
			return outers;
		}
		
		@Override
		public C29Bag build() {
			return this;
		}
		
		@Override
		public C29Bag.C29BagBuilder toBuilder() {
			C29Bag.C29BagBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C29Bag.C29BagBuilder builder) {
			ofNullable(getOuter()).ifPresent(builder::setOuter);
			ofNullable(getOuters()).ifPresent(builder::setOuters);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C29Bag _that = getType().cast(o);
		
			if (!Objects.equals(outer, _that.getOuter())) return false;
			if (!ListEquals.listEquals(outers, _that.getOuters())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (outer != null ? outer.hashCode() : 0);
			_result = 31 * _result + (outers != null ? outers.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C29Bag {" +
				"outer=" + this.outer + ", " +
				"outers=" + this.outers +
			'}';
		}
	}

	/*********************** Builder Implementation of C29Bag  ***********************/
	class C29BagBuilderImpl implements C29Bag.C29BagBuilder {
	
		protected C29Outer.C29OuterBuilder outer;
		protected List<C29Outer.C29OuterBuilder> outers = new ArrayList<>();
		
		@Override
		@RosettaAttribute("outer")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("outer")
		public C29Outer.C29OuterBuilder getOuter() {
			return outer;
		}
		
		@Override
		public C29Outer.C29OuterBuilder getOrCreateOuter() {
			C29Outer.C29OuterBuilder result;
			if (outer!=null) {
				result = outer;
			}
			else {
				result = outer = C29Outer.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("outers")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("outers")
		public List<? extends C29Outer.C29OuterBuilder> getOuters() {
			return outers;
		}
		
		@Override
		public C29Outer.C29OuterBuilder getOrCreateOuters(int index) {
			if (outers==null) {
				this.outers = new ArrayList<>();
			}
			return getIndex(outers, index, () -> {
						C29Outer.C29OuterBuilder newOuters = C29Outer.builder();
						return newOuters;
					});
		}
		
		@RosettaAttribute("outer")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("outer")
		@Override
		public C29Bag.C29BagBuilder setOuter(C29Outer _outer) {
			this.outer = _outer == null ? null : _outer.toBuilder();
			return this;
		}
		
		@RosettaAttribute("outers")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("outers")
		@Override
		public C29Bag.C29BagBuilder addOuters(C29Outer _outers) {
			if (_outers != null) {
				this.outers.add(_outers.toBuilder());
			}
			return this;
		}
		
		@Override
		public C29Bag.C29BagBuilder addOuters(C29Outer _outers, int idx) {
			getIndex(this.outers, idx, () -> _outers.toBuilder());
			return this;
		}
		
		@Override
		public C29Bag.C29BagBuilder addOuters(List<? extends C29Outer> outerss) {
			if (outerss != null) {
				for (final C29Outer toAdd : outerss) {
					this.outers.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("outers")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("outers")
		@Override
		public C29Bag.C29BagBuilder setOuters(List<? extends C29Outer> outerss) {
			if (outerss == null) {
				this.outers = new ArrayList<>();
			} else {
				this.outers = outerss.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public C29Bag build() {
			return new C29Bag.C29BagImpl(this);
		}
		
		@Override
		public C29Bag.C29BagBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C29Bag.C29BagBuilder prune() {
			if (outer!=null && !outer.prune().hasData()) outer = null;
			outers = outers.stream().filter(b->b!=null).<C29Outer.C29OuterBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getOuter()!=null && getOuter().hasData()) return true;
			if (getOuters()!=null && getOuters().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C29Bag.C29BagBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C29Bag.C29BagBuilder o = (C29Bag.C29BagBuilder) other;
			
			merger.mergeRosetta(getOuter(), o.getOuter(), this::setOuter);
			merger.mergeRosetta(getOuters(), o.getOuters(), this::getOrCreateOuters);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C29Bag _that = getType().cast(o);
		
			if (!Objects.equals(outer, _that.getOuter())) return false;
			if (!ListEquals.listEquals(outers, _that.getOuters())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (outer != null ? outer.hashCode() : 0);
			_result = 31 * _result + (outers != null ? outers.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C29BagBuilder {" +
				"outer=" + this.outer + ", " +
				"outers=" + this.outers +
			'}';
		}
	}
}

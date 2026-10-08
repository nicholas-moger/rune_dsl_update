package chaos.s26.a1o1;

import chaos.s26.a1o1.meta.C26BagMeta;
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
@RosettaDataType(value="C26Bag", builder=C26Bag.C26BagBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C26Bag", model="chaos", builder=C26Bag.C26BagBuilderImpl.class, version="1.0.0")
public interface C26Bag extends RosettaModelObject {

	C26BagMeta metaData = new C26BagMeta();

	/*********************** Getter Methods  ***********************/
	List<? extends C26Either> getEths();
	C26KindEnum getKind();

	/*********************** Build Methods  ***********************/
	C26Bag build();
	
	C26Bag.C26BagBuilder toBuilder();
	
	static C26Bag.C26BagBuilder builder() {
		return new C26Bag.C26BagBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C26Bag> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C26Bag> getType() {
		return C26Bag.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("eths"), processor, C26Either.class, getEths());
		processor.processBasic(path.newSubPath("kind"), C26KindEnum.class, getKind(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C26BagBuilder extends C26Bag, RosettaModelObjectBuilder {
		C26Either.C26EitherBuilder getOrCreateEths(int index);
		@Override
		List<? extends C26Either.C26EitherBuilder> getEths();
		C26Bag.C26BagBuilder addEths(C26Either eths);
		C26Bag.C26BagBuilder addEths(C26Either eths, int idx);
		C26Bag.C26BagBuilder addEths(List<? extends C26Either> eths);
		C26Bag.C26BagBuilder setEths(List<? extends C26Either> eths);
		C26Bag.C26BagBuilder setKind(C26KindEnum kind);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("eths"), processor, C26Either.C26EitherBuilder.class, getEths());
			processor.processBasic(path.newSubPath("kind"), C26KindEnum.class, getKind(), this);
		}
		

		C26Bag.C26BagBuilder prune();
	}

	/*********************** Immutable Implementation of C26Bag  ***********************/
	class C26BagImpl implements C26Bag {
		private final List<? extends C26Either> eths;
		private final C26KindEnum kind;
		
		protected C26BagImpl(C26Bag.C26BagBuilder builder) {
			this.eths = ofNullable(builder.getEths()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
			this.kind = builder.getKind();
		}
		
		@Override
		@RosettaAttribute("eths")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("eths")
		public List<? extends C26Either> getEths() {
			return eths;
		}
		
		@Override
		@RosettaAttribute("kind")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("kind")
		public C26KindEnum getKind() {
			return kind;
		}
		
		@Override
		public C26Bag build() {
			return this;
		}
		
		@Override
		public C26Bag.C26BagBuilder toBuilder() {
			C26Bag.C26BagBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C26Bag.C26BagBuilder builder) {
			ofNullable(getEths()).ifPresent(builder::setEths);
			ofNullable(getKind()).ifPresent(builder::setKind);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C26Bag _that = getType().cast(o);
		
			if (!ListEquals.listEquals(eths, _that.getEths())) return false;
			if (!Objects.equals(kind, _that.getKind())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (eths != null ? eths.hashCode() : 0);
			_result = 31 * _result + (kind != null ? kind.getClass().getName().hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C26Bag {" +
				"eths=" + this.eths + ", " +
				"kind=" + this.kind +
			'}';
		}
	}

	/*********************** Builder Implementation of C26Bag  ***********************/
	class C26BagBuilderImpl implements C26Bag.C26BagBuilder {
	
		protected List<C26Either.C26EitherBuilder> eths = new ArrayList<>();
		protected C26KindEnum kind;
		
		@Override
		@RosettaAttribute("eths")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("eths")
		public List<? extends C26Either.C26EitherBuilder> getEths() {
			return eths;
		}
		
		@Override
		public C26Either.C26EitherBuilder getOrCreateEths(int index) {
			if (eths==null) {
				this.eths = new ArrayList<>();
			}
			return getIndex(eths, index, () -> {
						C26Either.C26EitherBuilder newEths = C26Either.builder();
						return newEths;
					});
		}
		
		@Override
		@RosettaAttribute("kind")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("kind")
		public C26KindEnum getKind() {
			return kind;
		}
		
		@RosettaAttribute("eths")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("eths")
		@Override
		public C26Bag.C26BagBuilder addEths(C26Either _eths) {
			if (_eths != null) {
				this.eths.add(_eths.toBuilder());
			}
			return this;
		}
		
		@Override
		public C26Bag.C26BagBuilder addEths(C26Either _eths, int idx) {
			getIndex(this.eths, idx, () -> _eths.toBuilder());
			return this;
		}
		
		@Override
		public C26Bag.C26BagBuilder addEths(List<? extends C26Either> ethss) {
			if (ethss != null) {
				for (final C26Either toAdd : ethss) {
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
		public C26Bag.C26BagBuilder setEths(List<? extends C26Either> ethss) {
			if (ethss == null) {
				this.eths = new ArrayList<>();
			} else {
				this.eths = ethss.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("kind")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("kind")
		@Override
		public C26Bag.C26BagBuilder setKind(C26KindEnum _kind) {
			this.kind = _kind == null ? null : _kind;
			return this;
		}
		
		@Override
		public C26Bag build() {
			return new C26Bag.C26BagImpl(this);
		}
		
		@Override
		public C26Bag.C26BagBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C26Bag.C26BagBuilder prune() {
			eths = eths.stream().filter(b->b!=null).<C26Either.C26EitherBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getEths()!=null && getEths().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			if (getKind()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C26Bag.C26BagBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C26Bag.C26BagBuilder o = (C26Bag.C26BagBuilder) other;
			
			merger.mergeRosetta(getEths(), o.getEths(), this::getOrCreateEths);
			
			merger.mergeBasic(getKind(), o.getKind(), this::setKind);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C26Bag _that = getType().cast(o);
		
			if (!ListEquals.listEquals(eths, _that.getEths())) return false;
			if (!Objects.equals(kind, _that.getKind())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (eths != null ? eths.hashCode() : 0);
			_result = 31 * _result + (kind != null ? kind.getClass().getName().hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C26BagBuilder {" +
				"eths=" + this.eths + ", " +
				"kind=" + this.kind +
			'}';
		}
	}
}

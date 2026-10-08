package test.fmeta022;

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
import com.rosetta.model.metafields.ReferenceWithMetaString;
import com.rosetta.util.ListEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import test.fmeta022.meta.AMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="A", builder=A.ABuilderImpl.class, version="0.0.0")
@RuneDataType(value="A", model="test", builder=A.ABuilderImpl.class, version="0.0.0")
public interface A extends RosettaModelObject {

	AMeta metaData = new AMeta();

	/*********************** Getter Methods  ***********************/
	List<? extends ReferenceWithMetaString> getA();

	/*********************** Build Methods  ***********************/
	A build();
	
	A.ABuilder toBuilder();
	
	static A.ABuilder builder() {
		return new A.ABuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends A> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends A> getType() {
		return A.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("a"), processor, ReferenceWithMetaString.class, getA());
	}
	

	/*********************** Builder Interface  ***********************/
	interface ABuilder extends A, RosettaModelObjectBuilder {
		ReferenceWithMetaString.ReferenceWithMetaStringBuilder getOrCreateA(int index);
		@Override
		List<? extends ReferenceWithMetaString.ReferenceWithMetaStringBuilder> getA();
		A.ABuilder addA(ReferenceWithMetaString a);
		A.ABuilder addA(ReferenceWithMetaString a, int idx);
		A.ABuilder addAValue(String a);
		A.ABuilder addAValue(String a, int idx);
		A.ABuilder addA(List<? extends ReferenceWithMetaString> a);
		A.ABuilder setA(List<? extends ReferenceWithMetaString> a);
		A.ABuilder addAValue(List<? extends String> a);
		A.ABuilder setAValue(List<? extends String> a);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("a"), processor, ReferenceWithMetaString.ReferenceWithMetaStringBuilder.class, getA());
		}
		

		A.ABuilder prune();
	}

	/*********************** Immutable Implementation of A  ***********************/
	class AImpl implements A {
		private final List<? extends ReferenceWithMetaString> a;
		
		protected AImpl(A.ABuilder builder) {
			this.a = ofNullable(builder.getA()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
		}
		
		@Override
		@RosettaAttribute("a")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("a")
		public List<? extends ReferenceWithMetaString> getA() {
			return a;
		}
		
		@Override
		public A build() {
			return this;
		}
		
		@Override
		public A.ABuilder toBuilder() {
			A.ABuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(A.ABuilder builder) {
			ofNullable(getA()).ifPresent(builder::setA);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			A _that = getType().cast(o);
		
			if (!ListEquals.listEquals(a, _that.getA())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (a != null ? a.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "A {" +
				"a=" + this.a +
			'}';
		}
	}

	/*********************** Builder Implementation of A  ***********************/
	class ABuilderImpl implements A.ABuilder {
	
		protected List<ReferenceWithMetaString.ReferenceWithMetaStringBuilder> a = new ArrayList<>();
		
		@Override
		@RosettaAttribute("a")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("a")
		public List<? extends ReferenceWithMetaString.ReferenceWithMetaStringBuilder> getA() {
			return a;
		}
		
		@Override
		public ReferenceWithMetaString.ReferenceWithMetaStringBuilder getOrCreateA(int index) {
			if (a==null) {
				this.a = new ArrayList<>();
			}
			return getIndex(a, index, () -> {
						ReferenceWithMetaString.ReferenceWithMetaStringBuilder newA = ReferenceWithMetaString.builder();
						return newA;
					});
		}
		
		@RosettaAttribute("a")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("a")
		@Override
		public A.ABuilder addA(ReferenceWithMetaString _a) {
			if (_a != null) {
				this.a.add(_a.toBuilder());
			}
			return this;
		}
		
		@Override
		public A.ABuilder addA(ReferenceWithMetaString _a, int idx) {
			getIndex(this.a, idx, () -> _a.toBuilder());
			return this;
		}
		
		@Override
		public A.ABuilder addAValue(String _a) {
			this.getOrCreateA(-1).setValue(_a);
			return this;
		}
		
		@Override
		public A.ABuilder addAValue(String _a, int idx) {
			this.getOrCreateA(idx).setValue(_a);
			return this;
		}
		
		@Override
		public A.ABuilder addA(List<? extends ReferenceWithMetaString> as) {
			if (as != null) {
				for (final ReferenceWithMetaString toAdd : as) {
					this.a.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("a")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("a")
		@Override
		public A.ABuilder setA(List<? extends ReferenceWithMetaString> as) {
			if (as == null) {
				this.a = new ArrayList<>();
			} else {
				this.a = as.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public A.ABuilder addAValue(List<? extends String> as) {
			if (as != null) {
				for (final String toAdd : as) {
					this.addAValue(toAdd);
				}
			}
			return this;
		}
		
		@Override
		public A.ABuilder setAValue(List<? extends String> as) {
			this.a.clear();
			if (as != null) {
				as.forEach(this::addAValue);
			}
			return this;
		}
		
		@Override
		public A build() {
			return new A.AImpl(this);
		}
		
		@Override
		public A.ABuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public A.ABuilder prune() {
			a = a.stream().filter(b->b!=null).<ReferenceWithMetaString.ReferenceWithMetaStringBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getA()!=null && !getA().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public A.ABuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			A.ABuilder o = (A.ABuilder) other;
			
			merger.mergeRosetta(getA(), o.getA(), this::getOrCreateA);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			A _that = getType().cast(o);
		
			if (!ListEquals.listEquals(a, _that.getA())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (a != null ? a.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ABuilder {" +
				"a=" + this.a +
			'}';
		}
	}
}

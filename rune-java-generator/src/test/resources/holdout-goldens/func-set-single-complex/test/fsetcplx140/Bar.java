package test.fsetcplx140;

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
import test.fsetcplx140.meta.BarMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="Bar", builder=Bar.BarBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Bar", model="test", builder=Bar.BarBuilderImpl.class, version="0.0.0")
public interface Bar extends RosettaModelObject {

	BarMeta metaData = new BarMeta();

	/*********************** Getter Methods  ***********************/
	List<? extends Foo> getFoos();

	/*********************** Build Methods  ***********************/
	Bar build();
	
	Bar.BarBuilder toBuilder();
	
	static Bar.BarBuilder builder() {
		return new Bar.BarBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Bar> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Bar> getType() {
		return Bar.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("foos"), processor, Foo.class, getFoos());
	}
	

	/*********************** Builder Interface  ***********************/
	interface BarBuilder extends Bar, RosettaModelObjectBuilder {
		Foo.FooBuilder getOrCreateFoos(int index);
		@Override
		List<? extends Foo.FooBuilder> getFoos();
		Bar.BarBuilder addFoos(Foo foos);
		Bar.BarBuilder addFoos(Foo foos, int idx);
		Bar.BarBuilder addFoos(List<? extends Foo> foos);
		Bar.BarBuilder setFoos(List<? extends Foo> foos);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("foos"), processor, Foo.FooBuilder.class, getFoos());
		}
		

		Bar.BarBuilder prune();
	}

	/*********************** Immutable Implementation of Bar  ***********************/
	class BarImpl implements Bar {
		private final List<? extends Foo> foos;
		
		protected BarImpl(Bar.BarBuilder builder) {
			this.foos = ofNullable(builder.getFoos()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
		}
		
		@Override
		@RosettaAttribute("foos")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("foos")
		public List<? extends Foo> getFoos() {
			return foos;
		}
		
		@Override
		public Bar build() {
			return this;
		}
		
		@Override
		public Bar.BarBuilder toBuilder() {
			Bar.BarBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Bar.BarBuilder builder) {
			ofNullable(getFoos()).ifPresent(builder::setFoos);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Bar _that = getType().cast(o);
		
			if (!ListEquals.listEquals(foos, _that.getFoos())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (foos != null ? foos.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Bar {" +
				"foos=" + this.foos +
			'}';
		}
	}

	/*********************** Builder Implementation of Bar  ***********************/
	class BarBuilderImpl implements Bar.BarBuilder {
	
		protected List<Foo.FooBuilder> foos = new ArrayList<>();
		
		@Override
		@RosettaAttribute("foos")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("foos")
		public List<? extends Foo.FooBuilder> getFoos() {
			return foos;
		}
		
		@Override
		public Foo.FooBuilder getOrCreateFoos(int index) {
			if (foos==null) {
				this.foos = new ArrayList<>();
			}
			return getIndex(foos, index, () -> {
						Foo.FooBuilder newFoos = Foo.builder();
						return newFoos;
					});
		}
		
		@RosettaAttribute("foos")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("foos")
		@Override
		public Bar.BarBuilder addFoos(Foo _foos) {
			if (_foos != null) {
				this.foos.add(_foos.toBuilder());
			}
			return this;
		}
		
		@Override
		public Bar.BarBuilder addFoos(Foo _foos, int idx) {
			getIndex(this.foos, idx, () -> _foos.toBuilder());
			return this;
		}
		
		@Override
		public Bar.BarBuilder addFoos(List<? extends Foo> fooss) {
			if (fooss != null) {
				for (final Foo toAdd : fooss) {
					this.foos.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("foos")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("foos")
		@Override
		public Bar.BarBuilder setFoos(List<? extends Foo> fooss) {
			if (fooss == null) {
				this.foos = new ArrayList<>();
			} else {
				this.foos = fooss.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public Bar build() {
			return new Bar.BarImpl(this);
		}
		
		@Override
		public Bar.BarBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Bar.BarBuilder prune() {
			foos = foos.stream().filter(b->b!=null).<Foo.FooBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getFoos()!=null && getFoos().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Bar.BarBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Bar.BarBuilder o = (Bar.BarBuilder) other;
			
			merger.mergeRosetta(getFoos(), o.getFoos(), this::getOrCreateFoos);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Bar _that = getType().cast(o);
		
			if (!ListEquals.listEquals(foos, _that.getFoos())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (foos != null ? foos.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "BarBuilder {" +
				"foos=" + this.foos +
			'}';
		}
	}
}

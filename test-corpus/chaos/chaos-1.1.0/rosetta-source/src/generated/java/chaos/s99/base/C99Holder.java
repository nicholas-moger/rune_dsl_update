package chaos.s99.base;

import chaos.s99.base.meta.C99HolderMeta;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * A multi attribute of the shadowing alias (the type-format validator&#39;s List-of-Integer local) beside a single of the sibling.
 * @version 1.0.0
 */
@RosettaDataType(value="C99Holder", builder=C99Holder.C99HolderBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C99Holder", model="chaos", builder=C99Holder.C99HolderBuilderImpl.class, version="1.0.0")
public interface C99Holder extends RosettaModelObject {

	C99HolderMeta metaData = new C99HolderMeta();

	/*********************** Getter Methods  ***********************/
	List<Integer> getIg();
	Integer getN();

	/*********************** Build Methods  ***********************/
	C99Holder build();
	
	C99Holder.C99HolderBuilder toBuilder();
	
	static C99Holder.C99HolderBuilder builder() {
		return new C99Holder.C99HolderBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C99Holder> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C99Holder> getType() {
		return C99Holder.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("ig"), Integer.class, getIg(), this);
		processor.processBasic(path.newSubPath("n"), Integer.class, getN(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C99HolderBuilder extends C99Holder, RosettaModelObjectBuilder {
		C99Holder.C99HolderBuilder addIg(Integer ig);
		C99Holder.C99HolderBuilder addIg(Integer ig, int idx);
		C99Holder.C99HolderBuilder addIg(List<Integer> ig);
		C99Holder.C99HolderBuilder setIg(List<Integer> ig);
		C99Holder.C99HolderBuilder setN(Integer n);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("ig"), Integer.class, getIg(), this);
			processor.processBasic(path.newSubPath("n"), Integer.class, getN(), this);
		}
		

		C99Holder.C99HolderBuilder prune();
	}

	/*********************** Immutable Implementation of C99Holder  ***********************/
	class C99HolderImpl implements C99Holder {
		private final List<Integer> ig;
		private final Integer n;
		
		protected C99HolderImpl(C99Holder.C99HolderBuilder builder) {
			this.ig = ofNullable(builder.getIg()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.n = builder.getN();
		}
		
		@Override
		@RosettaAttribute("ig")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("ig")
		public List<Integer> getIg() {
			return ig;
		}
		
		@Override
		@RosettaAttribute("n")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("n")
		public Integer getN() {
			return n;
		}
		
		@Override
		public C99Holder build() {
			return this;
		}
		
		@Override
		public C99Holder.C99HolderBuilder toBuilder() {
			C99Holder.C99HolderBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C99Holder.C99HolderBuilder builder) {
			ofNullable(getIg()).ifPresent(builder::setIg);
			ofNullable(getN()).ifPresent(builder::setN);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C99Holder _that = getType().cast(o);
		
			if (!ListEquals.listEquals(ig, _that.getIg())) return false;
			if (!Objects.equals(n, _that.getN())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (ig != null ? ig.hashCode() : 0);
			_result = 31 * _result + (n != null ? n.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C99Holder {" +
				"ig=" + this.ig + ", " +
				"n=" + this.n +
			'}';
		}
	}

	/*********************** Builder Implementation of C99Holder  ***********************/
	class C99HolderBuilderImpl implements C99Holder.C99HolderBuilder {
	
		protected List<Integer> ig = new ArrayList<>();
		protected Integer n;
		
		@Override
		@RosettaAttribute("ig")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("ig")
		public List<Integer> getIg() {
			return ig;
		}
		
		@Override
		@RosettaAttribute("n")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("n")
		public Integer getN() {
			return n;
		}
		
		@RosettaAttribute("ig")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("ig")
		@Override
		public C99Holder.C99HolderBuilder addIg(Integer _ig) {
			if (_ig != null) {
				this.ig.add(_ig);
			}
			return this;
		}
		
		@Override
		public C99Holder.C99HolderBuilder addIg(Integer _ig, int idx) {
			getIndex(this.ig, idx, () -> _ig);
			return this;
		}
		
		@Override
		public C99Holder.C99HolderBuilder addIg(List<Integer> igs) {
			if (igs != null) {
				for (final Integer toAdd : igs) {
					this.ig.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("ig")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("ig")
		@Override
		public C99Holder.C99HolderBuilder setIg(List<Integer> igs) {
			if (igs == null) {
				this.ig = new ArrayList<>();
			} else {
				this.ig = igs.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("n")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("n")
		@Override
		public C99Holder.C99HolderBuilder setN(Integer _n) {
			this.n = _n == null ? null : _n;
			return this;
		}
		
		@Override
		public C99Holder build() {
			return new C99Holder.C99HolderImpl(this);
		}
		
		@Override
		public C99Holder.C99HolderBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C99Holder.C99HolderBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getIg()!=null && !getIg().isEmpty()) return true;
			if (getN()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C99Holder.C99HolderBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C99Holder.C99HolderBuilder o = (C99Holder.C99HolderBuilder) other;
			
			
			merger.mergeBasic(getIg(), o.getIg(), (Consumer<Integer>) this::addIg);
			merger.mergeBasic(getN(), o.getN(), this::setN);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C99Holder _that = getType().cast(o);
		
			if (!ListEquals.listEquals(ig, _that.getIg())) return false;
			if (!Objects.equals(n, _that.getN())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (ig != null ? ig.hashCode() : 0);
			_result = 31 * _result + (n != null ? n.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C99HolderBuilder {" +
				"ig=" + this.ig + ", " +
				"n=" + this.n +
			'}';
		}
	}
}

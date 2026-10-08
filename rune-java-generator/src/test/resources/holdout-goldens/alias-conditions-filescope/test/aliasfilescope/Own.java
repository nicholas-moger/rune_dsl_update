package test.aliasfilescope;

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
import test.aliasfilescope.meta.OwnMeta;

import static java.util.Optional.ofNullable;

/**
 * The validator OwnTypeFormatValidator wires a condition class of its OWN simple name - the file scope&#39;s first claim is the class itself.
 * @version 0.0.0
 */
@RosettaDataType(value="Own", builder=Own.OwnBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Own", model="test", builder=Own.OwnBuilderImpl.class, version="0.0.0")
public interface Own extends RosettaModelObject {

	OwnMeta metaData = new OwnMeta();

	/*********************** Getter Methods  ***********************/
	Integer getV();
	List<Integer> getVs();

	/*********************** Build Methods  ***********************/
	Own build();
	
	Own.OwnBuilder toBuilder();
	
	static Own.OwnBuilder builder() {
		return new Own.OwnBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Own> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Own> getType() {
		return Own.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("v"), Integer.class, getV(), this);
		processor.processBasic(path.newSubPath("vs"), Integer.class, getVs(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface OwnBuilder extends Own, RosettaModelObjectBuilder {
		Own.OwnBuilder setV(Integer v);
		Own.OwnBuilder addVs(Integer vs);
		Own.OwnBuilder addVs(Integer vs, int idx);
		Own.OwnBuilder addVs(List<Integer> vs);
		Own.OwnBuilder setVs(List<Integer> vs);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("v"), Integer.class, getV(), this);
			processor.processBasic(path.newSubPath("vs"), Integer.class, getVs(), this);
		}
		

		Own.OwnBuilder prune();
	}

	/*********************** Immutable Implementation of Own  ***********************/
	class OwnImpl implements Own {
		private final Integer v;
		private final List<Integer> vs;
		
		protected OwnImpl(Own.OwnBuilder builder) {
			this.v = builder.getV();
			this.vs = ofNullable(builder.getVs()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
		}
		
		@Override
		@RosettaAttribute("v")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("v")
		public Integer getV() {
			return v;
		}
		
		@Override
		@RosettaAttribute("vs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("vs")
		public List<Integer> getVs() {
			return vs;
		}
		
		@Override
		public Own build() {
			return this;
		}
		
		@Override
		public Own.OwnBuilder toBuilder() {
			Own.OwnBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Own.OwnBuilder builder) {
			ofNullable(getV()).ifPresent(builder::setV);
			ofNullable(getVs()).ifPresent(builder::setVs);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Own _that = getType().cast(o);
		
			if (!Objects.equals(v, _that.getV())) return false;
			if (!ListEquals.listEquals(vs, _that.getVs())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (v != null ? v.hashCode() : 0);
			_result = 31 * _result + (vs != null ? vs.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Own {" +
				"v=" + this.v + ", " +
				"vs=" + this.vs +
			'}';
		}
	}

	/*********************** Builder Implementation of Own  ***********************/
	class OwnBuilderImpl implements Own.OwnBuilder {
	
		protected Integer v;
		protected List<Integer> vs = new ArrayList<>();
		
		@Override
		@RosettaAttribute("v")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("v")
		public Integer getV() {
			return v;
		}
		
		@Override
		@RosettaAttribute("vs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("vs")
		public List<Integer> getVs() {
			return vs;
		}
		
		@RosettaAttribute("v")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("v")
		@Override
		public Own.OwnBuilder setV(Integer _v) {
			this.v = _v == null ? null : _v;
			return this;
		}
		
		@RosettaAttribute("vs")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("vs")
		@Override
		public Own.OwnBuilder addVs(Integer _vs) {
			if (_vs != null) {
				this.vs.add(_vs);
			}
			return this;
		}
		
		@Override
		public Own.OwnBuilder addVs(Integer _vs, int idx) {
			getIndex(this.vs, idx, () -> _vs);
			return this;
		}
		
		@Override
		public Own.OwnBuilder addVs(List<Integer> vss) {
			if (vss != null) {
				for (final Integer toAdd : vss) {
					this.vs.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("vs")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("vs")
		@Override
		public Own.OwnBuilder setVs(List<Integer> vss) {
			if (vss == null) {
				this.vs = new ArrayList<>();
			} else {
				this.vs = vss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public Own build() {
			return new Own.OwnImpl(this);
		}
		
		@Override
		public Own.OwnBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Own.OwnBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getV()!=null) return true;
			if (getVs()!=null && !getVs().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Own.OwnBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Own.OwnBuilder o = (Own.OwnBuilder) other;
			
			
			merger.mergeBasic(getV(), o.getV(), this::setV);
			merger.mergeBasic(getVs(), o.getVs(), (Consumer<Integer>) this::addVs);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Own _that = getType().cast(o);
		
			if (!Objects.equals(v, _that.getV())) return false;
			if (!ListEquals.listEquals(vs, _that.getVs())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (v != null ? v.hashCode() : 0);
			_result = 31 * _result + (vs != null ? vs.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "OwnBuilder {" +
				"v=" + this.v + ", " +
				"vs=" + this.vs +
			'}';
		}
	}
}

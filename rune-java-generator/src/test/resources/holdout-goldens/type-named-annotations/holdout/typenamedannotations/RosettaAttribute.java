package holdout.typenamedannotations;

import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.util.ListEquals;
import holdout.typenamedannotations.meta.RosettaAttributeMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * com.rosetta.model.lib.annotations.RosettaAttribute - the POJO&#39;s getter annotation.
 * @version 0.0.0
 */
@RosettaDataType(value="RosettaAttribute", builder=RosettaAttribute.RosettaAttributeBuilderImpl.class, version="0.0.0")
@RuneDataType(value="RosettaAttribute", model="holdout", builder=RosettaAttribute.RosettaAttributeBuilderImpl.class, version="0.0.0")
public interface RosettaAttribute extends RosettaModelObject {

	RosettaAttributeMeta metaData = new RosettaAttributeMeta();

	/*********************** Getter Methods  ***********************/
	List<String> getXs();
	String getX();

	/*********************** Build Methods  ***********************/
	RosettaAttribute build();
	
	RosettaAttribute.RosettaAttributeBuilder toBuilder();
	
	static RosettaAttribute.RosettaAttributeBuilder builder() {
		return new RosettaAttribute.RosettaAttributeBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends RosettaAttribute> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends RosettaAttribute> getType() {
		return RosettaAttribute.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("xs"), String.class, getXs(), this);
		processor.processBasic(path.newSubPath("x"), String.class, getX(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface RosettaAttributeBuilder extends RosettaAttribute, RosettaModelObjectBuilder {
		RosettaAttribute.RosettaAttributeBuilder addXs(String xs);
		RosettaAttribute.RosettaAttributeBuilder addXs(String xs, int idx);
		RosettaAttribute.RosettaAttributeBuilder addXs(List<String> xs);
		RosettaAttribute.RosettaAttributeBuilder setXs(List<String> xs);
		RosettaAttribute.RosettaAttributeBuilder setX(String x);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("xs"), String.class, getXs(), this);
			processor.processBasic(path.newSubPath("x"), String.class, getX(), this);
		}
		

		RosettaAttribute.RosettaAttributeBuilder prune();
	}

	/*********************** Immutable Implementation of RosettaAttribute  ***********************/
	class RosettaAttributeImpl implements RosettaAttribute {
		private final List<String> xs;
		private final String x;
		
		protected RosettaAttributeImpl(RosettaAttribute.RosettaAttributeBuilder builder) {
			this.xs = ofNullable(builder.getXs()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.x = builder.getX();
		}
		
		@Override
		@com.rosetta.model.lib.annotations.RosettaAttribute("xs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("xs")
		public List<String> getXs() {
			return xs;
		}
		
		@Override
		@com.rosetta.model.lib.annotations.RosettaAttribute("x")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("x")
		public String getX() {
			return x;
		}
		
		@Override
		public RosettaAttribute build() {
			return this;
		}
		
		@Override
		public RosettaAttribute.RosettaAttributeBuilder toBuilder() {
			RosettaAttribute.RosettaAttributeBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RosettaAttribute.RosettaAttributeBuilder builder) {
			ofNullable(getXs()).ifPresent(builder::setXs);
			ofNullable(getX()).ifPresent(builder::setX);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RosettaAttribute _that = getType().cast(o);
		
			if (!ListEquals.listEquals(xs, _that.getXs())) return false;
			if (!Objects.equals(x, _that.getX())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (xs != null ? xs.hashCode() : 0);
			_result = 31 * _result + (x != null ? x.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RosettaAttribute {" +
				"xs=" + this.xs + ", " +
				"x=" + this.x +
			'}';
		}
	}

	/*********************** Builder Implementation of RosettaAttribute  ***********************/
	class RosettaAttributeBuilderImpl implements RosettaAttribute.RosettaAttributeBuilder {
	
		protected List<String> xs = new ArrayList<>();
		protected String x;
		
		@Override
		@com.rosetta.model.lib.annotations.RosettaAttribute("xs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("xs")
		public List<String> getXs() {
			return xs;
		}
		
		@Override
		@com.rosetta.model.lib.annotations.RosettaAttribute("x")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("x")
		public String getX() {
			return x;
		}
		
		@com.rosetta.model.lib.annotations.RosettaAttribute("xs")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("xs")
		@Override
		public RosettaAttribute.RosettaAttributeBuilder addXs(String _xs) {
			if (_xs != null) {
				this.xs.add(_xs);
			}
			return this;
		}
		
		@Override
		public RosettaAttribute.RosettaAttributeBuilder addXs(String _xs, int idx) {
			getIndex(this.xs, idx, () -> _xs);
			return this;
		}
		
		@Override
		public RosettaAttribute.RosettaAttributeBuilder addXs(List<String> xss) {
			if (xss != null) {
				for (final String toAdd : xss) {
					this.xs.add(toAdd);
				}
			}
			return this;
		}
		
		@com.rosetta.model.lib.annotations.RosettaAttribute("xs")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("xs")
		@Override
		public RosettaAttribute.RosettaAttributeBuilder setXs(List<String> xss) {
			if (xss == null) {
				this.xs = new ArrayList<>();
			} else {
				this.xs = xss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@com.rosetta.model.lib.annotations.RosettaAttribute("x")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("x")
		@Override
		public RosettaAttribute.RosettaAttributeBuilder setX(String _x) {
			this.x = _x == null ? null : _x;
			return this;
		}
		
		@Override
		public RosettaAttribute build() {
			return new RosettaAttribute.RosettaAttributeImpl(this);
		}
		
		@Override
		public RosettaAttribute.RosettaAttributeBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RosettaAttribute.RosettaAttributeBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getXs()!=null && !getXs().isEmpty()) return true;
			if (getX()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RosettaAttribute.RosettaAttributeBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RosettaAttribute.RosettaAttributeBuilder o = (RosettaAttribute.RosettaAttributeBuilder) other;
			
			
			merger.mergeBasic(getXs(), o.getXs(), (Consumer<String>) this::addXs);
			merger.mergeBasic(getX(), o.getX(), this::setX);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RosettaAttribute _that = getType().cast(o);
		
			if (!ListEquals.listEquals(xs, _that.getXs())) return false;
			if (!Objects.equals(x, _that.getX())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (xs != null ? xs.hashCode() : 0);
			_result = 31 * _result + (x != null ? x.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RosettaAttributeBuilder {" +
				"xs=" + this.xs + ", " +
				"x=" + this.x +
			'}';
		}
	}
}

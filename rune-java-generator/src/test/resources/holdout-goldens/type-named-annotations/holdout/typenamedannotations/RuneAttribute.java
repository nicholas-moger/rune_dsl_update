package holdout.typenamedannotations;

import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.util.ListEquals;
import holdout.typenamedannotations.meta.RuneAttributeMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * com.rosetta.model.lib.annotations.RuneAttribute - the POJO&#39;s getter and getType annotation.
 * @version 0.0.0
 */
@RosettaDataType(value="RuneAttribute", builder=RuneAttribute.RuneAttributeBuilderImpl.class, version="0.0.0")
@RuneDataType(value="RuneAttribute", model="holdout", builder=RuneAttribute.RuneAttributeBuilderImpl.class, version="0.0.0")
public interface RuneAttribute extends RosettaModelObject {

	RuneAttributeMeta metaData = new RuneAttributeMeta();

	/*********************** Getter Methods  ***********************/
	List<String> getXs();
	String getX();

	/*********************** Build Methods  ***********************/
	RuneAttribute build();
	
	RuneAttribute.RuneAttributeBuilder toBuilder();
	
	static RuneAttribute.RuneAttributeBuilder builder() {
		return new RuneAttribute.RuneAttributeBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends RuneAttribute> metaData() {
		return metaData;
	}
	
	@Override
	@com.rosetta.model.lib.annotations.RuneAttribute("@type")
	default Class<? extends RuneAttribute> getType() {
		return RuneAttribute.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("xs"), String.class, getXs(), this);
		processor.processBasic(path.newSubPath("x"), String.class, getX(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface RuneAttributeBuilder extends RuneAttribute, RosettaModelObjectBuilder {
		RuneAttribute.RuneAttributeBuilder addXs(String xs);
		RuneAttribute.RuneAttributeBuilder addXs(String xs, int idx);
		RuneAttribute.RuneAttributeBuilder addXs(List<String> xs);
		RuneAttribute.RuneAttributeBuilder setXs(List<String> xs);
		RuneAttribute.RuneAttributeBuilder setX(String x);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("xs"), String.class, getXs(), this);
			processor.processBasic(path.newSubPath("x"), String.class, getX(), this);
		}
		

		RuneAttribute.RuneAttributeBuilder prune();
	}

	/*********************** Immutable Implementation of RuneAttribute  ***********************/
	class RuneAttributeImpl implements RuneAttribute {
		private final List<String> xs;
		private final String x;
		
		protected RuneAttributeImpl(RuneAttribute.RuneAttributeBuilder builder) {
			this.xs = ofNullable(builder.getXs()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.x = builder.getX();
		}
		
		@Override
		@RosettaAttribute("xs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@com.rosetta.model.lib.annotations.RuneAttribute("xs")
		public List<String> getXs() {
			return xs;
		}
		
		@Override
		@RosettaAttribute("x")
		@Accessor(AccessorType.GETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("x")
		public String getX() {
			return x;
		}
		
		@Override
		public RuneAttribute build() {
			return this;
		}
		
		@Override
		public RuneAttribute.RuneAttributeBuilder toBuilder() {
			RuneAttribute.RuneAttributeBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RuneAttribute.RuneAttributeBuilder builder) {
			ofNullable(getXs()).ifPresent(builder::setXs);
			ofNullable(getX()).ifPresent(builder::setX);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RuneAttribute _that = getType().cast(o);
		
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
			return "RuneAttribute {" +
				"xs=" + this.xs + ", " +
				"x=" + this.x +
			'}';
		}
	}

	/*********************** Builder Implementation of RuneAttribute  ***********************/
	class RuneAttributeBuilderImpl implements RuneAttribute.RuneAttributeBuilder {
	
		protected List<String> xs = new ArrayList<>();
		protected String x;
		
		@Override
		@RosettaAttribute("xs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@com.rosetta.model.lib.annotations.RuneAttribute("xs")
		public List<String> getXs() {
			return xs;
		}
		
		@Override
		@RosettaAttribute("x")
		@Accessor(AccessorType.GETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("x")
		public String getX() {
			return x;
		}
		
		@RosettaAttribute("xs")
		@Accessor(AccessorType.ADDER)
		@Multi
		@com.rosetta.model.lib.annotations.RuneAttribute("xs")
		@Override
		public RuneAttribute.RuneAttributeBuilder addXs(String _xs) {
			if (_xs != null) {
				this.xs.add(_xs);
			}
			return this;
		}
		
		@Override
		public RuneAttribute.RuneAttributeBuilder addXs(String _xs, int idx) {
			getIndex(this.xs, idx, () -> _xs);
			return this;
		}
		
		@Override
		public RuneAttribute.RuneAttributeBuilder addXs(List<String> xss) {
			if (xss != null) {
				for (final String toAdd : xss) {
					this.xs.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("xs")
		@Accessor(AccessorType.SETTER)
		@Multi
		@com.rosetta.model.lib.annotations.RuneAttribute("xs")
		@Override
		public RuneAttribute.RuneAttributeBuilder setXs(List<String> xss) {
			if (xss == null) {
				this.xs = new ArrayList<>();
			} else {
				this.xs = xss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("x")
		@Accessor(AccessorType.SETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("x")
		@Override
		public RuneAttribute.RuneAttributeBuilder setX(String _x) {
			this.x = _x == null ? null : _x;
			return this;
		}
		
		@Override
		public RuneAttribute build() {
			return new RuneAttribute.RuneAttributeImpl(this);
		}
		
		@Override
		public RuneAttribute.RuneAttributeBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RuneAttribute.RuneAttributeBuilder prune() {
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
		public RuneAttribute.RuneAttributeBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RuneAttribute.RuneAttributeBuilder o = (RuneAttribute.RuneAttributeBuilder) other;
			
			
			merger.mergeBasic(getXs(), o.getXs(), (Consumer<String>) this::addXs);
			merger.mergeBasic(getX(), o.getX(), this::setX);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RuneAttribute _that = getType().cast(o);
		
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
			return "RuneAttributeBuilder {" +
				"xs=" + this.xs + ", " +
				"x=" + this.x +
			'}';
		}
	}
}

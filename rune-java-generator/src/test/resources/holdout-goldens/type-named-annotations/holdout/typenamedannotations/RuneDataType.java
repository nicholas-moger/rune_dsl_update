package holdout.typenamedannotations;

import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.util.ListEquals;
import holdout.typenamedannotations.meta.RuneDataTypeMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * com.rosetta.model.lib.annotations.RuneDataType - the POJO&#39;s class annotation.
 * @version 0.0.0
 */
@RosettaDataType(value="RuneDataType", builder=RuneDataType.RuneDataTypeBuilderImpl.class, version="0.0.0")
@com.rosetta.model.lib.annotations.RuneDataType(value="RuneDataType", model="holdout", builder=RuneDataType.RuneDataTypeBuilderImpl.class, version="0.0.0")
public interface RuneDataType extends RosettaModelObject {

	RuneDataTypeMeta metaData = new RuneDataTypeMeta();

	/*********************** Getter Methods  ***********************/
	List<String> getXs();
	String getX();

	/*********************** Build Methods  ***********************/
	RuneDataType build();
	
	RuneDataType.RuneDataTypeBuilder toBuilder();
	
	static RuneDataType.RuneDataTypeBuilder builder() {
		return new RuneDataType.RuneDataTypeBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends RuneDataType> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends RuneDataType> getType() {
		return RuneDataType.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("xs"), String.class, getXs(), this);
		processor.processBasic(path.newSubPath("x"), String.class, getX(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface RuneDataTypeBuilder extends RuneDataType, RosettaModelObjectBuilder {
		RuneDataType.RuneDataTypeBuilder addXs(String xs);
		RuneDataType.RuneDataTypeBuilder addXs(String xs, int idx);
		RuneDataType.RuneDataTypeBuilder addXs(List<String> xs);
		RuneDataType.RuneDataTypeBuilder setXs(List<String> xs);
		RuneDataType.RuneDataTypeBuilder setX(String x);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("xs"), String.class, getXs(), this);
			processor.processBasic(path.newSubPath("x"), String.class, getX(), this);
		}
		

		RuneDataType.RuneDataTypeBuilder prune();
	}

	/*********************** Immutable Implementation of RuneDataType  ***********************/
	class RuneDataTypeImpl implements RuneDataType {
		private final List<String> xs;
		private final String x;
		
		protected RuneDataTypeImpl(RuneDataType.RuneDataTypeBuilder builder) {
			this.xs = ofNullable(builder.getXs()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.x = builder.getX();
		}
		
		@Override
		@RosettaAttribute("xs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("xs")
		public List<String> getXs() {
			return xs;
		}
		
		@Override
		@RosettaAttribute("x")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("x")
		public String getX() {
			return x;
		}
		
		@Override
		public RuneDataType build() {
			return this;
		}
		
		@Override
		public RuneDataType.RuneDataTypeBuilder toBuilder() {
			RuneDataType.RuneDataTypeBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RuneDataType.RuneDataTypeBuilder builder) {
			ofNullable(getXs()).ifPresent(builder::setXs);
			ofNullable(getX()).ifPresent(builder::setX);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RuneDataType _that = getType().cast(o);
		
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
			return "RuneDataType {" +
				"xs=" + this.xs + ", " +
				"x=" + this.x +
			'}';
		}
	}

	/*********************** Builder Implementation of RuneDataType  ***********************/
	class RuneDataTypeBuilderImpl implements RuneDataType.RuneDataTypeBuilder {
	
		protected List<String> xs = new ArrayList<>();
		protected String x;
		
		@Override
		@RosettaAttribute("xs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("xs")
		public List<String> getXs() {
			return xs;
		}
		
		@Override
		@RosettaAttribute("x")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("x")
		public String getX() {
			return x;
		}
		
		@RosettaAttribute("xs")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("xs")
		@Override
		public RuneDataType.RuneDataTypeBuilder addXs(String _xs) {
			if (_xs != null) {
				this.xs.add(_xs);
			}
			return this;
		}
		
		@Override
		public RuneDataType.RuneDataTypeBuilder addXs(String _xs, int idx) {
			getIndex(this.xs, idx, () -> _xs);
			return this;
		}
		
		@Override
		public RuneDataType.RuneDataTypeBuilder addXs(List<String> xss) {
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
		@RuneAttribute("xs")
		@Override
		public RuneDataType.RuneDataTypeBuilder setXs(List<String> xss) {
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
		@RuneAttribute("x")
		@Override
		public RuneDataType.RuneDataTypeBuilder setX(String _x) {
			this.x = _x == null ? null : _x;
			return this;
		}
		
		@Override
		public RuneDataType build() {
			return new RuneDataType.RuneDataTypeImpl(this);
		}
		
		@Override
		public RuneDataType.RuneDataTypeBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RuneDataType.RuneDataTypeBuilder prune() {
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
		public RuneDataType.RuneDataTypeBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RuneDataType.RuneDataTypeBuilder o = (RuneDataType.RuneDataTypeBuilder) other;
			
			
			merger.mergeBasic(getXs(), o.getXs(), (Consumer<String>) this::addXs);
			merger.mergeBasic(getX(), o.getX(), this::setX);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RuneDataType _that = getType().cast(o);
		
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
			return "RuneDataTypeBuilder {" +
				"xs=" + this.xs + ", " +
				"x=" + this.x +
			'}';
		}
	}
}

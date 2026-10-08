package holdout.typenamedrosetta;

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
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.util.ListEquals;
import holdout.typenamedrosetta.meta.BuilderMergerMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * com.rosetta.model.lib.process.BuilderMerger - written by the POJO builder&#39;s merge method.
 * @version 0.0.0
 */
@RosettaDataType(value="BuilderMerger", builder=BuilderMerger.BuilderMergerBuilderImpl.class, version="0.0.0")
@RuneDataType(value="BuilderMerger", model="holdout", builder=BuilderMerger.BuilderMergerBuilderImpl.class, version="0.0.0")
public interface BuilderMerger extends RosettaModelObject {

	BuilderMergerMeta metaData = new BuilderMergerMeta();

	/*********************** Getter Methods  ***********************/
	List<String> getXs();
	String getX();

	/*********************** Build Methods  ***********************/
	BuilderMerger build();
	
	BuilderMerger.BuilderMergerBuilder toBuilder();
	
	static BuilderMerger.BuilderMergerBuilder builder() {
		return new BuilderMerger.BuilderMergerBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends BuilderMerger> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends BuilderMerger> getType() {
		return BuilderMerger.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("xs"), String.class, getXs(), this);
		processor.processBasic(path.newSubPath("x"), String.class, getX(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface BuilderMergerBuilder extends BuilderMerger, RosettaModelObjectBuilder {
		BuilderMerger.BuilderMergerBuilder addXs(String xs);
		BuilderMerger.BuilderMergerBuilder addXs(String xs, int idx);
		BuilderMerger.BuilderMergerBuilder addXs(List<String> xs);
		BuilderMerger.BuilderMergerBuilder setXs(List<String> xs);
		BuilderMerger.BuilderMergerBuilder setX(String x);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("xs"), String.class, getXs(), this);
			processor.processBasic(path.newSubPath("x"), String.class, getX(), this);
		}
		

		BuilderMerger.BuilderMergerBuilder prune();
	}

	/*********************** Immutable Implementation of BuilderMerger  ***********************/
	class BuilderMergerImpl implements BuilderMerger {
		private final List<String> xs;
		private final String x;
		
		protected BuilderMergerImpl(BuilderMerger.BuilderMergerBuilder builder) {
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
		public BuilderMerger build() {
			return this;
		}
		
		@Override
		public BuilderMerger.BuilderMergerBuilder toBuilder() {
			BuilderMerger.BuilderMergerBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(BuilderMerger.BuilderMergerBuilder builder) {
			ofNullable(getXs()).ifPresent(builder::setXs);
			ofNullable(getX()).ifPresent(builder::setX);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			BuilderMerger _that = getType().cast(o);
		
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
			return "BuilderMerger {" +
				"xs=" + this.xs + ", " +
				"x=" + this.x +
			'}';
		}
	}

	/*********************** Builder Implementation of BuilderMerger  ***********************/
	class BuilderMergerBuilderImpl implements BuilderMerger.BuilderMergerBuilder {
	
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
		public BuilderMerger.BuilderMergerBuilder addXs(String _xs) {
			if (_xs != null) {
				this.xs.add(_xs);
			}
			return this;
		}
		
		@Override
		public BuilderMerger.BuilderMergerBuilder addXs(String _xs, int idx) {
			getIndex(this.xs, idx, () -> _xs);
			return this;
		}
		
		@Override
		public BuilderMerger.BuilderMergerBuilder addXs(List<String> xss) {
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
		public BuilderMerger.BuilderMergerBuilder setXs(List<String> xss) {
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
		public BuilderMerger.BuilderMergerBuilder setX(String _x) {
			this.x = _x == null ? null : _x;
			return this;
		}
		
		@Override
		public BuilderMerger build() {
			return new BuilderMerger.BuilderMergerImpl(this);
		}
		
		@Override
		public BuilderMerger.BuilderMergerBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public BuilderMerger.BuilderMergerBuilder prune() {
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
		public BuilderMerger.BuilderMergerBuilder merge(RosettaModelObjectBuilder other, com.rosetta.model.lib.process.BuilderMerger merger) {
			BuilderMerger.BuilderMergerBuilder o = (BuilderMerger.BuilderMergerBuilder) other;
			
			
			merger.mergeBasic(getXs(), o.getXs(), (Consumer<String>) this::addXs);
			merger.mergeBasic(getX(), o.getX(), this::setX);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			BuilderMerger _that = getType().cast(o);
		
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
			return "BuilderMergerBuilder {" +
				"xs=" + this.xs + ", " +
				"x=" + this.x +
			'}';
		}
	}
}

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
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.util.ListEquals;
import holdout.typenamedannotations.meta.RequiredMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * com.rosetta.model.lib.annotations.Required - the POJO&#39;s annotation on a required getter.
 * @version 0.0.0
 */
@RosettaDataType(value="Required", builder=Required.RequiredBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Required", model="holdout", builder=Required.RequiredBuilderImpl.class, version="0.0.0")
public interface Required extends RosettaModelObject {

	RequiredMeta metaData = new RequiredMeta();

	/*********************** Getter Methods  ***********************/
	List<String> getXs();
	String getX();

	/*********************** Build Methods  ***********************/
	Required build();
	
	Required.RequiredBuilder toBuilder();
	
	static Required.RequiredBuilder builder() {
		return new Required.RequiredBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Required> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Required> getType() {
		return Required.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("xs"), String.class, getXs(), this);
		processor.processBasic(path.newSubPath("x"), String.class, getX(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface RequiredBuilder extends Required, RosettaModelObjectBuilder {
		Required.RequiredBuilder addXs(String xs);
		Required.RequiredBuilder addXs(String xs, int idx);
		Required.RequiredBuilder addXs(List<String> xs);
		Required.RequiredBuilder setXs(List<String> xs);
		Required.RequiredBuilder setX(String x);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("xs"), String.class, getXs(), this);
			processor.processBasic(path.newSubPath("x"), String.class, getX(), this);
		}
		

		Required.RequiredBuilder prune();
	}

	/*********************** Immutable Implementation of Required  ***********************/
	class RequiredImpl implements Required {
		private final List<String> xs;
		private final String x;
		
		protected RequiredImpl(Required.RequiredBuilder builder) {
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
		@com.rosetta.model.lib.annotations.Required
		@RuneAttribute("x")
		public String getX() {
			return x;
		}
		
		@Override
		public Required build() {
			return this;
		}
		
		@Override
		public Required.RequiredBuilder toBuilder() {
			Required.RequiredBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Required.RequiredBuilder builder) {
			ofNullable(getXs()).ifPresent(builder::setXs);
			ofNullable(getX()).ifPresent(builder::setX);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Required _that = getType().cast(o);
		
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
			return "Required {" +
				"xs=" + this.xs + ", " +
				"x=" + this.x +
			'}';
		}
	}

	/*********************** Builder Implementation of Required  ***********************/
	class RequiredBuilderImpl implements Required.RequiredBuilder {
	
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
		@com.rosetta.model.lib.annotations.Required
		@RuneAttribute("x")
		public String getX() {
			return x;
		}
		
		@RosettaAttribute("xs")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("xs")
		@Override
		public Required.RequiredBuilder addXs(String _xs) {
			if (_xs != null) {
				this.xs.add(_xs);
			}
			return this;
		}
		
		@Override
		public Required.RequiredBuilder addXs(String _xs, int idx) {
			getIndex(this.xs, idx, () -> _xs);
			return this;
		}
		
		@Override
		public Required.RequiredBuilder addXs(List<String> xss) {
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
		public Required.RequiredBuilder setXs(List<String> xss) {
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
		@com.rosetta.model.lib.annotations.Required
		@RuneAttribute("x")
		@Override
		public Required.RequiredBuilder setX(String _x) {
			this.x = _x == null ? null : _x;
			return this;
		}
		
		@Override
		public Required build() {
			return new Required.RequiredImpl(this);
		}
		
		@Override
		public Required.RequiredBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Required.RequiredBuilder prune() {
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
		public Required.RequiredBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Required.RequiredBuilder o = (Required.RequiredBuilder) other;
			
			
			merger.mergeBasic(getXs(), o.getXs(), (Consumer<String>) this::addXs);
			merger.mergeBasic(getX(), o.getX(), this::setX);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Required _that = getType().cast(o);
		
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
			return "RequiredBuilder {" +
				"xs=" + this.xs + ", " +
				"x=" + this.x +
			'}';
		}
	}
}

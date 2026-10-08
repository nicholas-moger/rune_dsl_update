package holdout.typenamedrosetta;

import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
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
import holdout.typenamedrosetta.meta.RosettaModelObjectBuilderMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * com.rosetta.model.lib.RosettaModelObjectBuilder - the POJO builder&#39;s super-interface.
 * @version 0.0.0
 */
@RosettaDataType(value="RosettaModelObjectBuilder", builder=RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilderImpl.class, version="0.0.0")
@RuneDataType(value="RosettaModelObjectBuilder", model="holdout", builder=RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilderImpl.class, version="0.0.0")
public interface RosettaModelObjectBuilder extends RosettaModelObject {

	RosettaModelObjectBuilderMeta metaData = new RosettaModelObjectBuilderMeta();

	/*********************** Getter Methods  ***********************/
	List<String> getXs();
	String getX();

	/*********************** Build Methods  ***********************/
	RosettaModelObjectBuilder build();
	
	RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder toBuilder();
	
	static RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder builder() {
		return new RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends RosettaModelObjectBuilder> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends RosettaModelObjectBuilder> getType() {
		return RosettaModelObjectBuilder.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("xs"), String.class, getXs(), this);
		processor.processBasic(path.newSubPath("x"), String.class, getX(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface RosettaModelObjectBuilderBuilder extends RosettaModelObjectBuilder, com.rosetta.model.lib.RosettaModelObjectBuilder {
		RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder addXs(String xs);
		RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder addXs(String xs, int idx);
		RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder addXs(List<String> xs);
		RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder setXs(List<String> xs);
		RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder setX(String x);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("xs"), String.class, getXs(), this);
			processor.processBasic(path.newSubPath("x"), String.class, getX(), this);
		}
		

		RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder prune();
	}

	/*********************** Immutable Implementation of RosettaModelObjectBuilder  ***********************/
	class RosettaModelObjectBuilderImpl implements RosettaModelObjectBuilder {
		private final List<String> xs;
		private final String x;
		
		protected RosettaModelObjectBuilderImpl(RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder builder) {
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
		public RosettaModelObjectBuilder build() {
			return this;
		}
		
		@Override
		public RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder toBuilder() {
			RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder builder) {
			ofNullable(getXs()).ifPresent(builder::setXs);
			ofNullable(getX()).ifPresent(builder::setX);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RosettaModelObjectBuilder _that = getType().cast(o);
		
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
			return "RosettaModelObjectBuilder {" +
				"xs=" + this.xs + ", " +
				"x=" + this.x +
			'}';
		}
	}

	/*********************** Builder Implementation of RosettaModelObjectBuilder  ***********************/
	class RosettaModelObjectBuilderBuilderImpl implements RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder {
	
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
		public RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder addXs(String _xs) {
			if (_xs != null) {
				this.xs.add(_xs);
			}
			return this;
		}
		
		@Override
		public RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder addXs(String _xs, int idx) {
			getIndex(this.xs, idx, () -> _xs);
			return this;
		}
		
		@Override
		public RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder addXs(List<String> xss) {
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
		public RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder setXs(List<String> xss) {
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
		public RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder setX(String _x) {
			this.x = _x == null ? null : _x;
			return this;
		}
		
		@Override
		public RosettaModelObjectBuilder build() {
			return new RosettaModelObjectBuilder.RosettaModelObjectBuilderImpl(this);
		}
		
		@Override
		public RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder prune() {
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
		public RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder merge(com.rosetta.model.lib.RosettaModelObjectBuilder other, BuilderMerger merger) {
			RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder o = (RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder) other;
			
			
			merger.mergeBasic(getXs(), o.getXs(), (Consumer<String>) this::addXs);
			merger.mergeBasic(getX(), o.getX(), this::setX);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RosettaModelObjectBuilder _that = getType().cast(o);
		
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
			return "RosettaModelObjectBuilderBuilder {" +
				"xs=" + this.xs + ", " +
				"x=" + this.x +
			'}';
		}
	}
}

package holdout.typenamedrosetta;

import com.google.common.collect.ImmutableList;
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
import holdout.typenamedrosetta.meta.RosettaModelObjectMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * com.rosetta.model.lib.RosettaModelObject - the POJO&#39;s own super-interface.
 * @version 0.0.0
 */
@RosettaDataType(value="RosettaModelObject", builder=RosettaModelObject.RosettaModelObjectBuilderImpl.class, version="0.0.0")
@RuneDataType(value="RosettaModelObject", model="holdout", builder=RosettaModelObject.RosettaModelObjectBuilderImpl.class, version="0.0.0")
public interface RosettaModelObject extends com.rosetta.model.lib.RosettaModelObject {

	RosettaModelObjectMeta metaData = new RosettaModelObjectMeta();

	/*********************** Getter Methods  ***********************/
	List<String> getXs();
	String getX();

	/*********************** Build Methods  ***********************/
	RosettaModelObject build();
	
	RosettaModelObject.RosettaModelObjectBuilder toBuilder();
	
	static RosettaModelObject.RosettaModelObjectBuilder builder() {
		return new RosettaModelObject.RosettaModelObjectBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends RosettaModelObject> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends RosettaModelObject> getType() {
		return RosettaModelObject.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("xs"), String.class, getXs(), this);
		processor.processBasic(path.newSubPath("x"), String.class, getX(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface RosettaModelObjectBuilder extends RosettaModelObject, RosettaModelObjectBuilder {
		RosettaModelObject.RosettaModelObjectBuilder addXs(String xs);
		RosettaModelObject.RosettaModelObjectBuilder addXs(String xs, int idx);
		RosettaModelObject.RosettaModelObjectBuilder addXs(List<String> xs);
		RosettaModelObject.RosettaModelObjectBuilder setXs(List<String> xs);
		RosettaModelObject.RosettaModelObjectBuilder setX(String x);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("xs"), String.class, getXs(), this);
			processor.processBasic(path.newSubPath("x"), String.class, getX(), this);
		}
		

		RosettaModelObject.RosettaModelObjectBuilder prune();
	}

	/*********************** Immutable Implementation of RosettaModelObject  ***********************/
	class RosettaModelObjectImpl implements RosettaModelObject {
		private final List<String> xs;
		private final String x;
		
		protected RosettaModelObjectImpl(RosettaModelObject.RosettaModelObjectBuilder builder) {
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
		public RosettaModelObject build() {
			return this;
		}
		
		@Override
		public RosettaModelObject.RosettaModelObjectBuilder toBuilder() {
			RosettaModelObject.RosettaModelObjectBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RosettaModelObject.RosettaModelObjectBuilder builder) {
			ofNullable(getXs()).ifPresent(builder::setXs);
			ofNullable(getX()).ifPresent(builder::setX);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof com.rosetta.model.lib.RosettaModelObject) || !getType().equals(((com.rosetta.model.lib.RosettaModelObject)o).getType())) return false;
		
			RosettaModelObject _that = getType().cast(o);
		
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
			return "RosettaModelObject {" +
				"xs=" + this.xs + ", " +
				"x=" + this.x +
			'}';
		}
	}

	/*********************** Builder Implementation of RosettaModelObject  ***********************/
	class RosettaModelObjectBuilderImpl implements RosettaModelObject.RosettaModelObjectBuilder {
	
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
		public RosettaModelObject.RosettaModelObjectBuilder addXs(String _xs) {
			if (_xs != null) {
				this.xs.add(_xs);
			}
			return this;
		}
		
		@Override
		public RosettaModelObject.RosettaModelObjectBuilder addXs(String _xs, int idx) {
			getIndex(this.xs, idx, () -> _xs);
			return this;
		}
		
		@Override
		public RosettaModelObject.RosettaModelObjectBuilder addXs(List<String> xss) {
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
		public RosettaModelObject.RosettaModelObjectBuilder setXs(List<String> xss) {
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
		public RosettaModelObject.RosettaModelObjectBuilder setX(String _x) {
			this.x = _x == null ? null : _x;
			return this;
		}
		
		@Override
		public RosettaModelObject build() {
			return new RosettaModelObject.RosettaModelObjectImpl(this);
		}
		
		@Override
		public RosettaModelObject.RosettaModelObjectBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RosettaModelObject.RosettaModelObjectBuilder prune() {
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
		public RosettaModelObject.RosettaModelObjectBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RosettaModelObject.RosettaModelObjectBuilder o = (RosettaModelObject.RosettaModelObjectBuilder) other;
			
			
			merger.mergeBasic(getXs(), o.getXs(), (Consumer<String>) this::addXs);
			merger.mergeBasic(getX(), o.getX(), this::setX);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof com.rosetta.model.lib.RosettaModelObject) || !getType().equals(((com.rosetta.model.lib.RosettaModelObject)o).getType())) return false;
		
			RosettaModelObject _that = getType().cast(o);
		
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
}

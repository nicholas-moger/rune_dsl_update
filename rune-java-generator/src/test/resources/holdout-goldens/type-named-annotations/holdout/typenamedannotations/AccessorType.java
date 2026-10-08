package holdout.typenamedannotations;

import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
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
import holdout.typenamedannotations.meta.AccessorTypeMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * com.rosetta.model.lib.annotations.AccessorType - the POJO&#39;s getter annotation argument.
 * @version 0.0.0
 */
@RosettaDataType(value="AccessorType", builder=AccessorType.AccessorTypeBuilderImpl.class, version="0.0.0")
@RuneDataType(value="AccessorType", model="holdout", builder=AccessorType.AccessorTypeBuilderImpl.class, version="0.0.0")
public interface AccessorType extends RosettaModelObject {

	AccessorTypeMeta metaData = new AccessorTypeMeta();

	/*********************** Getter Methods  ***********************/
	List<String> getXs();
	String getX();

	/*********************** Build Methods  ***********************/
	AccessorType build();
	
	AccessorType.AccessorTypeBuilder toBuilder();
	
	static AccessorType.AccessorTypeBuilder builder() {
		return new AccessorType.AccessorTypeBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends AccessorType> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends AccessorType> getType() {
		return AccessorType.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("xs"), String.class, getXs(), this);
		processor.processBasic(path.newSubPath("x"), String.class, getX(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface AccessorTypeBuilder extends AccessorType, RosettaModelObjectBuilder {
		AccessorType.AccessorTypeBuilder addXs(String xs);
		AccessorType.AccessorTypeBuilder addXs(String xs, int idx);
		AccessorType.AccessorTypeBuilder addXs(List<String> xs);
		AccessorType.AccessorTypeBuilder setXs(List<String> xs);
		AccessorType.AccessorTypeBuilder setX(String x);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("xs"), String.class, getXs(), this);
			processor.processBasic(path.newSubPath("x"), String.class, getX(), this);
		}
		

		AccessorType.AccessorTypeBuilder prune();
	}

	/*********************** Immutable Implementation of AccessorType  ***********************/
	class AccessorTypeImpl implements AccessorType {
		private final List<String> xs;
		private final String x;
		
		protected AccessorTypeImpl(AccessorType.AccessorTypeBuilder builder) {
			this.xs = ofNullable(builder.getXs()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.x = builder.getX();
		}
		
		@Override
		@RosettaAttribute("xs")
		@Accessor(com.rosetta.model.lib.annotations.AccessorType.GETTER)
		@Multi
		@RuneAttribute("xs")
		public List<String> getXs() {
			return xs;
		}
		
		@Override
		@RosettaAttribute("x")
		@Accessor(com.rosetta.model.lib.annotations.AccessorType.GETTER)
		@RuneAttribute("x")
		public String getX() {
			return x;
		}
		
		@Override
		public AccessorType build() {
			return this;
		}
		
		@Override
		public AccessorType.AccessorTypeBuilder toBuilder() {
			AccessorType.AccessorTypeBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(AccessorType.AccessorTypeBuilder builder) {
			ofNullable(getXs()).ifPresent(builder::setXs);
			ofNullable(getX()).ifPresent(builder::setX);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			AccessorType _that = getType().cast(o);
		
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
			return "AccessorType {" +
				"xs=" + this.xs + ", " +
				"x=" + this.x +
			'}';
		}
	}

	/*********************** Builder Implementation of AccessorType  ***********************/
	class AccessorTypeBuilderImpl implements AccessorType.AccessorTypeBuilder {
	
		protected List<String> xs = new ArrayList<>();
		protected String x;
		
		@Override
		@RosettaAttribute("xs")
		@Accessor(com.rosetta.model.lib.annotations.AccessorType.GETTER)
		@Multi
		@RuneAttribute("xs")
		public List<String> getXs() {
			return xs;
		}
		
		@Override
		@RosettaAttribute("x")
		@Accessor(com.rosetta.model.lib.annotations.AccessorType.GETTER)
		@RuneAttribute("x")
		public String getX() {
			return x;
		}
		
		@RosettaAttribute("xs")
		@Accessor(com.rosetta.model.lib.annotations.AccessorType.ADDER)
		@Multi
		@RuneAttribute("xs")
		@Override
		public AccessorType.AccessorTypeBuilder addXs(String _xs) {
			if (_xs != null) {
				this.xs.add(_xs);
			}
			return this;
		}
		
		@Override
		public AccessorType.AccessorTypeBuilder addXs(String _xs, int idx) {
			getIndex(this.xs, idx, () -> _xs);
			return this;
		}
		
		@Override
		public AccessorType.AccessorTypeBuilder addXs(List<String> xss) {
			if (xss != null) {
				for (final String toAdd : xss) {
					this.xs.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("xs")
		@Accessor(com.rosetta.model.lib.annotations.AccessorType.SETTER)
		@Multi
		@RuneAttribute("xs")
		@Override
		public AccessorType.AccessorTypeBuilder setXs(List<String> xss) {
			if (xss == null) {
				this.xs = new ArrayList<>();
			} else {
				this.xs = xss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("x")
		@Accessor(com.rosetta.model.lib.annotations.AccessorType.SETTER)
		@RuneAttribute("x")
		@Override
		public AccessorType.AccessorTypeBuilder setX(String _x) {
			this.x = _x == null ? null : _x;
			return this;
		}
		
		@Override
		public AccessorType build() {
			return new AccessorType.AccessorTypeImpl(this);
		}
		
		@Override
		public AccessorType.AccessorTypeBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public AccessorType.AccessorTypeBuilder prune() {
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
		public AccessorType.AccessorTypeBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			AccessorType.AccessorTypeBuilder o = (AccessorType.AccessorTypeBuilder) other;
			
			
			merger.mergeBasic(getXs(), o.getXs(), (Consumer<String>) this::addXs);
			merger.mergeBasic(getX(), o.getX(), this::setX);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			AccessorType _that = getType().cast(o);
		
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
			return "AccessorTypeBuilder {" +
				"xs=" + this.xs + ", " +
				"x=" + this.x +
			'}';
		}
	}
}

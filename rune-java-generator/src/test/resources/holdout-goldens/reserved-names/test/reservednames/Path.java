package test.reservednames;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;
import test.reservednames.meta.PathMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="Path", builder=Path.PathBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Path", model="test", builder=Path.PathBuilderImpl.class, version="0.0.0")
public interface Path extends RosettaModelObject {

	PathMeta metaData = new PathMeta();

	/*********************** Getter Methods  ***********************/
	String getName();

	/*********************** Build Methods  ***********************/
	Path build();
	
	Path.PathBuilder toBuilder();
	
	static Path.PathBuilder builder() {
		return new Path.PathBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Path> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Path> getType() {
		return Path.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("name"), String.class, getName(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface PathBuilder extends Path, RosettaModelObjectBuilder {
		Path.PathBuilder setName(String name);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("name"), String.class, getName(), this);
		}
		

		Path.PathBuilder prune();
	}

	/*********************** Immutable Implementation of Path  ***********************/
	class PathImpl implements Path {
		private final String name;
		
		protected PathImpl(Path.PathBuilder builder) {
			this.name = builder.getName();
		}
		
		@Override
		@RosettaAttribute("name")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("name")
		public String getName() {
			return name;
		}
		
		@Override
		public Path build() {
			return this;
		}
		
		@Override
		public Path.PathBuilder toBuilder() {
			Path.PathBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Path.PathBuilder builder) {
			ofNullable(getName()).ifPresent(builder::setName);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Path _that = getType().cast(o);
		
			if (!Objects.equals(name, _that.getName())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (name != null ? name.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Path {" +
				"name=" + this.name +
			'}';
		}
	}

	/*********************** Builder Implementation of Path  ***********************/
	class PathBuilderImpl implements Path.PathBuilder {
	
		protected String name;
		
		@Override
		@RosettaAttribute("name")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("name")
		public String getName() {
			return name;
		}
		
		@RosettaAttribute("name")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("name")
		@Override
		public Path.PathBuilder setName(String _name) {
			this.name = _name == null ? null : _name;
			return this;
		}
		
		@Override
		public Path build() {
			return new Path.PathImpl(this);
		}
		
		@Override
		public Path.PathBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Path.PathBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getName()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Path.PathBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Path.PathBuilder o = (Path.PathBuilder) other;
			
			
			merger.mergeBasic(getName(), o.getName(), this::setName);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Path _that = getType().cast(o);
		
			if (!Objects.equals(name, _that.getName())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (name != null ? name.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "PathBuilder {" +
				"name=" + this.name +
			'}';
		}
	}
}
